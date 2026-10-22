package com.example.bff.restclient.application.usecase

import com.example.bff.restclient.application.dto.DashboardOrderResult
import com.example.bff.restclient.application.dto.DashboardProductResult
import com.example.bff.restclient.application.dto.DashboardResult
import com.example.bff.restclient.application.dto.DashboardUserResult
import com.example.bff.restclient.domain.exception.UserNotFoundException
import com.example.bff.restclient.domain.model.Dashboard
import com.example.bff.restclient.domain.model.OrderLine
import com.example.bff.restclient.domain.model.UserId
import com.example.bff.restclient.domain.repository.OrderRepository
import com.example.bff.restclient.domain.repository.ProductRepository
import com.example.bff.restclient.domain.repository.UserRepository
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.Executor

/**
 * ダッシュボード画面向けのデータを組み立てるユースケース（application 層）。
 *
 * 下流の 3 系統（ユーザー・注文・商品）は、ドメインのポート（リポジトリ）越しに同期的に取得する。
 * 並列化はこのユースケースが専用の [Executor] 上で行う（ポートの実装は並列化を知らない）。
 *
 * 並列化は 2 段構え。
 *  1. ユーザーと注文を同時に投入し、両方の結果を待つ。
 *  2. 注文の件数分の商品取得を同時に投入し、待つ。商品 ID が注文の結果に依存するため、1 段目の後に始まる。
 * よって待ち時間は「段 1 の遅い方」＋「段 2 の遅い方」になる（プールの待ち時間を除く）。
 *
 * 失敗の扱い（判定はこのユースケース、HTTP への変換は presentation）。
 *  - ユーザーが存在しない → UserNotFoundException。注文の失敗より優先する（user を先に待つため）。
 *  - それ以外の下流の失敗 → リポジトリが投げた DownstreamException をそのまま上げる。
 *  - 空白のみのユーザー ID は、下流へ問い合わせる前に UserNotFoundException とする。
 */
@Service
class GetDashboardUseCase(
    private val userRepository: UserRepository,
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val downstreamExecutor: Executor,
) {
    /**
     * ダッシュボードを組み立てる。
     *
     * user を orders より先に待つ。そのため user が 404 なら、orders の結果を待たずに例外が上がる。
     * ただし orders の呼び出しは既に投入済みで、キャンセルはしない。結果は捨てられるが、
     * 実行が終わるまでプールのスレッドは使われ続ける（404 時にも無駄な下流呼び出しが発生する点は認識しておく）。
     *
     * 商品は注文ごとに 1 回ずつ取得する（同じ商品を複数の注文が指していても重複排除しない）。
     * 途中の商品取得が失敗すると、その時点で例外が上がる。残りの商品取得は走り続ける（キャンセルしない）。
     *
     * @param rawUserId パスから受けたままのユーザー ID。値オブジェクトにできない場合は UserNotFoundException。
     */
    fun execute(rawUserId: String): DashboardResult {
        val userId = toUserId(rawUserId)

        // 段 1: 両方を先に投入してから待つ。投入と待ちの間に await を挟むと、直列になってしまう。
        val userFuture = supplyAsync { userRepository.findById(userId) }
        val ordersFuture = supplyAsync { orderRepository.findByUserId(userId) }

        // user を先に待つ。ここで UserNotFound が出れば、下の await(ordersFuture) には到達しない。
        val user = await(userFuture)
        val orders = await(ordersFuture)

        // 段 2: 注文件数分の商品取得を一斉に投入する。注文が 0 件なら取得は発生しない。
        val productFutures = orders.map { order ->
            order to supplyAsync { productRepository.findById(order.productId) }
        }
        // map は入力順を保つので、注文の並びは orders のまま維持される。
        val lines = productFutures.map { (order, productFuture) ->
            OrderLine(order = order, product = await(productFuture))
        }

        return Dashboard(user = user, orderLines = lines).toResult()
    }

    /**
     * パスの文字列を値オブジェクトにする。空白のみの ID は「存在しないユーザー」として扱う。
     * 下流へ問い合わせる前に判定するため、無駄な下流呼び出しは発生しない。
     */
    private fun toUserId(rawUserId: String): UserId =
        try {
            UserId(rawUserId)
        } catch (e: IllegalArgumentException) {
            throw UserNotFoundException(rawUserId, e)
        }

    /**
     * 専用の [downstreamExecutor] 上で非同期に実行する。
     * 既定の ForkJoinPool.commonPool を使わないのは、下流の待ち（I/O ブロッキング）で
     * 共有プールを占有しないため。そして同時実行数を `downstream.parallelism` で制御するため。
     */
    private fun <T> supplyAsync(call: () -> T): CompletableFuture<T> =
        CompletableFuture.supplyAsync(call, downstreamExecutor)

    /**
     * future の結果を待つ。
     *
     * [CompletableFuture.join] は失敗を [CompletionException] で包んで投げる。包んだままだと
     * GlobalExceptionHandler の例外型（UserNotFoundException / DownstreamException）に一致せず、想定外の 500 になる。
     * そこで元の例外（cause）を取り出して投げ直す。cause が無い場合は包みをそのまま投げる（通常は起きない防御的な分岐）。
     */
    private fun <T> await(future: CompletableFuture<T>): T =
        try {
            future.join()
        } catch (e: CompletionException) {
            throw e.cause ?: e
        }

    /** 集約を、アプリケーションの結果 DTO（プリミティブの形）へ写す。 */
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
