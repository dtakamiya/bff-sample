package com.example.bff.webclient.domain.model

/**
 * ドメインの識別子（値オブジェクト）。ドメイン層の最内側に置き、下流の表現（String の ID）をそのまま持ち込まない。
 *
 * - @JvmInline value class のため、実行時は String と同じ表現でラップのコストは無い。
 * - init で空白だけの ID を拒否する。型が分かれているので、UserId の場所に ProductId を渡す誤りはコンパイルで防がれる。
 * - 検査は isNotBlank のみ。桁数や文字種は下流の仕様が決めるため、ここでは縛らない。
 */
@JvmInline
value class UserId(val value: String) {
    init {
        requireNotBlankId(value, "UserId")
    }
}

/** 商品の識別子。検査の内容は UserId と同じ（空白のみを拒否）。 */
@JvmInline
value class ProductId(val value: String) {
    init {
        requireNotBlankId(value, "ProductId")
    }
}

/** 注文の識別子。検査の内容は UserId と同じ（空白のみを拒否）。 */
@JvmInline
value class OrderId(val value: String) {
    init {
        requireNotBlankId(value, "OrderId")
    }
}

private fun requireNotBlankId(value: String, typeName: String) {
    require(value.isNotBlank()) { "$typeName must not be blank" }
}
