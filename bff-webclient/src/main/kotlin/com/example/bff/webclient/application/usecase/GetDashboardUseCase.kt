package com.example.bff.webclient.application.usecase

import com.example.bff.webclient.application.dto.DashboardOrderResult
import com.example.bff.webclient.application.dto.DashboardProductResult
import com.example.bff.webclient.application.dto.DashboardResult
import com.example.bff.webclient.application.dto.DashboardUserResult
import com.example.bff.webclient.domain.exception.DomainException
import com.example.bff.webclient.domain.exception.toDomainFailure
import com.example.bff.webclient.domain.model.Dashboard
import com.example.bff.webclient.domain.model.Order
import com.example.bff.webclient.domain.model.Product
import com.example.bff.webclient.domain.model.ProductId
import com.example.bff.webclient.domain.model.UserId
import com.example.bff.webclient.domain.repository.OrderRepository
import com.example.bff.webclient.domain.repository.ProductRepository
import com.example.bff.webclient.domain.repository.UserRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * ダッシュボード画面向けのデータを組み立てるユースケース（アプリケーション層）。
 *
 * 下流の 3 系統（ユーザー・注文・商品）は、ドメインのポート（リポジトリ）越しに取得する。
 * 下流の実装（WebClient など）は知らない。取得した結果を Dashboard 集約にまとめ、結果 DTO として返す。
 *
 * 並列化の構造は 2 段構え。
 *  1. ユーザーと注文を Mono.zip で並列に取得し、両方が終わるまで待つ。
 *  2. 注文から商品 ID を集め、Flux.flatMap で商品詳細を並列に取得する。商品 ID が注文の結果に依存するため、1 段目の後に始まる。
 * よって所要時間はおおむね max(user, orders) + max(products)。各呼び出しは下流アダプタの timeout で打ち切られる。
 *
 * 失敗の扱いは次のとおり（判定はこのユースケースが行い、HTTP への変換は presentation が行う）。
 *  - ユーザーが存在しない → UserNotFoundException。他の失敗より優先して判定する。
 *  - それ以外の失敗（ユーザー・注文・商品のいずれか）→ DownstreamFailureException。
 */
@Service
class GetDashboardUseCase(
    private val userRepository: UserRepository,
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
) {
    /**
     * ダッシュボードを組み立てる。戻り値は Mono で、購読されて初めて下流呼び出しが始まる。
     *
     * - user と orders は settle で「成功か失敗か」を値に包んでから zip する。
     *   settle しないと Mono.zip は最初のエラーで残りをキャンセルする。その結果、どちらの失敗が返るかが
     *   到着順（タイミング）で変わってしまう。settle すると両方の完了を必ず待ち、判定は flatMap 内で決定的に行える。
     * - 副作用として、user が早く失敗しても orders の完了（最大 timeout）は待つ。
     * - 注意: 空の Mono（下流が本文なしで応答した等）は Outcome にならず、そのまま空として流れる。
     *   Mono.zip は source のどれかが空だと値を出さずに空で完了するため、この Mono も空になる。
     *   presentation で block() すると null になり、requireNotNull が IllegalArgumentException を投げ、
     *   BffExceptionHandler の対象外のため 500 になる。
     */
    fun execute(userId: UserId): Mono<DashboardResult> =
        Mono.zip(
            settle(userRepository.findById(userId)),
            settle(orderRepository.findByUserId(userId)),
        ) { userOutcome, ordersOutcome -> userOutcome to ordersOutcome }
            .flatMap { (userOutcome, ordersOutcome) ->
                // ユーザーの失敗を最優先で判定する。ここで UserNotFound なら orders の結果に関係なく 404 になる。
                val user = userOutcome.valueOrThrow()
                val orders = ordersOutcome.valueOrThrow()
                // 2 段目。注文が確定した後に、商品をまとめて取得する。
                fetchProducts(orders).map { products -> Dashboard.assemble(user, orders, products) }
            }
            .map { dashboard -> dashboard.toResult() }

    /**
     * 注文に含まれる商品を並列に取得し、ProductId をキーにしたマップにして返す。
     *
     * - distinct で同じ商品の重複取得を防ぐ（同じ商品が複数の注文にあっても 1 回だけ呼ぶ）。
     * - flatMap は各商品の呼び出しを並列に購読する。同時実行数は Reactor の既定値に任せている。
     * - キーはリクエストした ProductId を使う（取得結果の内容には依存しない）。
     * - collectMap は要素が 0 件でも空のマップで完了する。注文が無ければ、ここは空で完了する。
     * - 商品の失敗は（404 を含め）DownstreamFailureException になる。アダプタで変換済みだが、念のため揃える。
     */
    private fun fetchProducts(orders: List<Order>): Mono<Map<ProductId, Product>> =
        Flux.fromIterable(orders.map { it.productId }.distinct())
            .flatMap { productId -> productRepository.findById(productId).map { productId to it } }
            .collectMap({ it.first }, { it.second })
            .onErrorMap { it.toDomainFailure() }

    /**
     * 下流呼び出しの結果を「成功」か「失敗（ドメイン例外を保持）」のどちらかの値として表す型。
     *
     * Mono のエラーを一度「値」として運ぶための箱。エラーを値にしてしまえば、Mono.zip が途中で打ち切られない。
     * 判定（どの失敗を優先するか）は、zip の後段の valueOrThrow で行う。
     */
    private sealed interface Outcome<out T> {
        data class Success<out T>(val value: T) : Outcome<T>
        data class Failure(val error: DomainException) : Outcome<Nothing>
    }

    /**
     * Mono の成功・失敗を Outcome に包む。例外は onErrorResume で値に変換するため、エラーは外へ流れない。
     *
     * 注意: 空の Mono は成功にも失敗にもならず、空のまま流れる（map も onErrorResume も呼ばれない）。
     * そのため空応答は settle では救われず、上位の Mono.zip を空にする（execute の注意点を参照）。
     */
    private fun <T : Any> settle(call: Mono<T>): Mono<Outcome<T>> =
        call.map<Outcome<T>> { Outcome.Success(it) }
            .onErrorResume { Mono.just(Outcome.Failure(it.toDomainFailure())) }

    /** 成功なら値を返し、失敗ならそのドメイン例外を投げる。flatMap のラムダ内で投げた例外は Mono のエラーとして流れる。 */
    private fun <T : Any> Outcome<T>.valueOrThrow(): T = when (this) {
        is Outcome.Success -> value
        is Outcome.Failure -> throw error
    }

    /** 取得済みの Dashboard を、アプリケーションの結果 DTO（プリミティブの形）へ写す。 */
    private fun Dashboard.toResult(): DashboardResult =
        DashboardResult(
            user = DashboardUserResult(id = user.id.value, name = user.name, tier = user.tier),
            orders = orderLines.map { line ->
                DashboardOrderResult(
                    orderId = line.order.id.value,
                    quantity = line.order.quantity,
                    product = DashboardProductResult(
                        id = line.product.id.value,
                        name = line.product.name,
                        price = line.product.price,
                    ),
                )
            },
            totalAmount = totalAmount,
        )
}
