package com.example.bff.restclient.sequential.application.usecase

import com.example.bff.restclient.sequential.application.dto.DashboardOrderResult
import com.example.bff.restclient.sequential.application.dto.DashboardProductResult
import com.example.bff.restclient.sequential.application.dto.DashboardResult
import com.example.bff.restclient.sequential.application.dto.DashboardUserResult
import com.example.bff.restclient.sequential.domain.exception.UserNotFoundException
import com.example.bff.restclient.sequential.domain.model.Dashboard
import com.example.bff.restclient.sequential.domain.model.Order
import com.example.bff.restclient.sequential.domain.model.Product
import com.example.bff.restclient.sequential.domain.model.ProductId
import com.example.bff.restclient.sequential.domain.model.UserId
import com.example.bff.restclient.sequential.domain.repository.OrderRepository
import com.example.bff.restclient.sequential.domain.repository.ProductRepository
import com.example.bff.restclient.sequential.domain.repository.UserRepository
import org.springframework.stereotype.Service

/**
 * ダッシュボード画面向けのデータを組み立てるユースケース（逐次版、アプリケーション層）。
 *
 * 呼び出し順は次のとおり。並列版と違い、すべて呼び出しスレッド上で 1 つずつ実行する。
 *  1. user（存在確認を兼ねる）。404 ならここで終わり、orders と商品は呼ばれない。
 *  2. orders。失敗ならここで終わり、商品は呼ばれない。
 *  3. 注文ごとに product。注文数 N に対して N 回。同じ productId が複数の注文にあっても重複排除はしない。
 *
 * 下流の実装（RestClient など）は知らない。ポート越しに取得した結果を Dashboard 集約にまとめ、結果 DTO として返す。
 * 失敗の変換（404 → UserNotFoundException、その他 → DownstreamException）はアダプタが済ませているので、
 * ここでは例外を捕まえず、そのまま上へ流す。HTTP への変換は presentation が行う。
 */
@Service
class GetDashboardUseCase(
    private val userRepository: UserRepository,
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
) {
    /**
     * 生のパス変数（ユーザー ID）からダッシュボードを組み立てる。
     *
     * 空白のみの ID は値オブジェクトを作れないため、下流を呼ばずに [UserNotFoundException] とする
     * （存在しない user と同じ 404 になる）。
     */
    fun execute(rawUserId: String): DashboardResult {
        val userId = toUserId(rawUserId)
        val user = userRepository.findById(userId)
        val orders = orderRepository.findByUserId(userId)
        val products = fetchProducts(orders)
        return Dashboard.assemble(user, orders, products).toResult()
    }

    /** 生の ID を値オブジェクトにする。生成に失敗したら、存在しない user として扱う。 */
    private fun toUserId(rawUserId: String): UserId =
        try {
            UserId(rawUserId)
        } catch (e: IllegalArgumentException) {
            throw UserNotFoundException(rawUserId, e)
        }

    /**
     * 注文ごとに商品を 1 件ずつ取得し、ProductId をキーにしたマップにして返す。
     *
     * `associate` は注文の数だけラムダを評価するので、重複した productId も注文ごとに 1 回ずつ下流を呼ぶ（N+1 の形）。
     * 同じ商品が重なった場合は同じ中身が後勝ちで入るだけで、結果は変わらない。
     */
    private fun fetchProducts(orders: List<Order>): Map<ProductId, Product> =
        orders.associate { order -> order.productId to productRepository.findById(order.productId) }

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
