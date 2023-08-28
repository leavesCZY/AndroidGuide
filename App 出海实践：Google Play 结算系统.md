> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>  
> 希望对你有所帮助 🤣🤣🤣


现如今，App 出海热度不减，是很多公司和个人开发者选择的一个市场方向。App 为了实现盈利，除了接入广告这种最常见的变现方式外，就是通过提供各类虚拟商品或者是会员服务来吸引用户付费了，此时 Google Play 结算系统（Google Play's billing system）就是 Android 端应用必须使用到的一个支付渠道了

Google 对 Google Play 结算系统的简介：Google Play's billing system is a service that enables you to sell digital products and content in your Android app, whether you want to monetize through one-time purchases or offer subscriptions to your services. Google Play offers a full set of APIs for integration with both your Android app and your server backend that unlock the familiarity and safety of Google Play purchases for your users.

也就是说：Google Play 结算系统是一项可以让我们在 Android 应用中销售数字商品和内容的服务。无论是要通过一次性购买交易创收，还是要为用户提供订阅服务，它都能帮我们搞定。Google Play 提供了一整套 API，可集成到 Android 应用和服务器后端中，从而为用户提供熟悉又安全的 Google Play 购买交易服务

在最近的一年多时间里，我一直在负责一个海外项目的开发工作，这个过程中也接入了 Google Play 结算系统。在刚开始时，由于对当中的各个概念不够了解，其整体支付流程又和国内常用的各类支付服务相差挺大的，导致我走了不少的弯路

这里我就来写一篇文章，对 Google Play 结算系统进行详细介绍，希望对你有所帮助

# 一、概述

想要通过 Google Play 结算系统向用户展示并售卖商品，自然需要先创建商品，创建商品的方式有两种：

- 在 Google Play Console 手动创建
- 通过 Google Play Developer API 以代码的方式创建

在 Google Play 中创建的商品都属于虚拟商品，每个商品代表的都是 App 给用户提供的一种权益，而每个商品都包含一个唯一标识，也即 ProductId，我们在业务上就需要根据 ProductId 的命名规则来定义商品所代表的具体权益类型

每个商品又可以分为两种类型：

- 一次性商品。用户通过单次付费获得的商品，属于买断制，对应 Google Play 结算库中的 `BillingClient.ProductType.INAPP`
- 订阅型商品。用户以固定周期不断重复付费的商品，属于订阅制，对应 Google Play 结算库中的 `BillingClient.ProductType.SUBS`

当用户购买了商品后，App 还需要对这笔订单进行核销。处理流程和商品类型有关，分为两种：

- 确认交易。不管购买的商品是什么类型，App 都需要先对这笔交易进行 **确认**，如果在限定的时间内未完成确认，Google Play 就会自动撤销这笔交易并向用户退款。“确认交易” 这个操作应该是 Google Play 为了让 App 确定已经向用户提供了权益，尽量避免出现用户已付款但 App 没有向用户下发权益这种情况。确认操作可以由服务端或者移动端来实现，对应 `acknowledgePurchase` 操作
- 消耗商品。消耗商品针对的是一次性商品中的消耗型商品，也即对其执行 **消耗** 操作。通过执行消耗操作，使得用户后续可以再次购买此商品。消耗操作可以由服务端或者移动端来实现，对应 `consumePurchase` 操作

# 二、一次性商品

一次性商品也称为应用内商品，属于一次性买断的商品，具体又可以细分为两种子类型：

- 消耗型商品。也即是说，此商品在购买后可以被消耗，从而使得用户可以重复购买。例如，该商品可以用于表示游戏中的金币，用户在使用完金币后该商品代表的权益就失效了，用户需要再次购买商品才能再次获得金币
- 非消耗型商品。也即是说，此商品在购买后是不可消耗的，用户可以永久获得该商品代表的权益。例如，该商品可以用于表示某课程的观看权益，用户只要购买商品后，就可以永久享有该课程的观看权益

一次性商品到底属于 **消耗型** 还是 **非消耗型** 都取决于 App 在业务上的定义，在 Google Play Console 中都统一将其称为 **应用内商品**，在创建一次性商品时也没有区分子类型的选项

假设我们对一件一次性商品在业务上的定义是消耗型的，那么就可以在适当的时候通过执行 `consumePurchase` 来对其执行 “消耗” 操作。例如，用户通过购买某个一次性商品获得了游戏金币，用户在后续过程中使用这些金币来购买游戏道具，那么开发者就需要同时执行 `consumePurchase` 来消耗掉商品，从而使得该商品变为无效状态，这样用户后续也可以再次购买此商品

而对于非消耗型商品，在业务上代表的是用户可以永久享有的某个权益，只要买了该商品权益就不会丢失，因此用户也不应该再次购买，自然也就不需要也不能执行消耗操作了

# 三、订阅型商品

订阅型商品，也即需要用户以固定周期定期进行付费的商品，在付费周期内用户均能享有该商品代表的权益。最常见的应用场景就是各类会员服务：用户按月付费，App 在每个订阅周期内向用户提供会员独有的功能，直至用户取消订阅

订阅型商品包含四个比较重要的概念：

- 基础方案
- 续订类型
- 优惠
- 定价阶段

## 基础方案

基础方案，也称为 BasePaln，每个订阅型商品都必须包含一个或多个基础方案才能让用户购买

基础方案就用于定义商品的售卖规则，包括结算周期、续订类型、订阅价格、优惠策略等。例如，一个订阅型商品可以同时提供 **按月付费** 和 **按年付费** 这两个基础方案供用户选择，每个周期分别设定不同的价格，用户根据喜好来选择不同的方案进行订阅

## 续订类型

每个基础方案均需要指定续订类型，用于指定用户的付费方式

续订类型分为两种：

- 自动续订。在每个结算周期即将结束时主动向用户扣款，从而自动延长权益使用权的期限。付费操作对于用户来说是被动的
- 预付费。不会自动续订和扣款，用户需要通过主动付款来推迟权益使用权的结束日期，以此保持不间断地享有订阅内容。付费操作对于用户来说是主动的

## 优惠

优惠，也称为 Offer，只有 **自动续订型** 的基础方案才能设定优惠

每个自动续订型的基础方案可以同时设定多个优惠，让用户可以在订阅初期享受一定的价格折扣或者是直接就免费使用，从而吸引用户购买

Offer 的类型分为三种，也即分为三种优惠策略。例如，假设现在有一个按月订阅的基础方案，我们就可以为其添加以下三个 Offer 供用户选择：

- 免费试订。用户在前七天内免费试用，在七天后再正式进行按月付费
- 单次付款。用户一次性预付三个月的订阅费用，总价享受七折折扣，三个月后再按原价进行按月订阅
- 周期性付款折扣。用户还是按月订阅，但前三个月每次付费时均能享受八折折扣，三个月后再按原价进行按月订阅

## 价格阶段

价格阶段，也称为 PricingPhases，可以看做是 Offer 的一个内部属性

由于一个 Offer 可以同时包含多个优惠策略，所以当用户在享用某个 Offer 时，其需要支出的价格就会随时间发生多次变动，每个时间段分别对应的不同的价格，PricingPhases 就用于表示 Offer 在每一个时间段的收费规则

例如，某个按月自动续订的基础方案包含一个 Offer，此 Offer 包含一个七天免费试订的优惠策略。那么，此 Offer 的价格阶段就分别是：

- 用户先享受七天的免费试订
- 七天后，用户再按原价按月付费

假如为这个 Offer 再添加一个 “折扣为七折，为期一个月的周期性付款” 的优惠策略，此时 Offer 的价格阶段就变成了：

- 用户先享受七天的免费试订
- 七天后，用户按原价的七折进行付费，获得一个月的订阅期
- 一个月后，用户再按原价按月付费

所以说，价格阶段就决定了用户在不同时间段下所需要支出的费用，每个 Offer 最多允许添加两个价格阶段，也即最多发生三次价格变动，用户会按顺序来接收价格变化

## 总结

Google Play 设定 BasePlan 和 Offer 的自由度很高。自动续订的 BasePlan 的付费周期可以从一周到一年，预付费的 BasePlan 的付费周期可以从一天到一年。每种优惠策略的优惠周期和优惠价也都可以很灵活地设定。我们可以通过设定多种不同的周期时长和优惠策略供用户选择，从而尽量提高用户的付费率

此外，每个订阅型商品最多可以创建 250 个基础方案和优惠，但同时启用的基础方案和优惠不能超过 50 个，多出的基础方案和优惠必须处于草稿或未启用状态

# 四、Billing SDK

了解了以上的基础概念后，再来看这些概念如何和 Billing SDK 对应起来

本文所有的代码示例使用的均是当前 Google Play 结算系统在 Android 端最新版本的 SDK，且是协程版本，读者需要对协程有一定了解

```kotlin
dependencies {
    val billingVersion = "6.0.1"
    implementation("com.android.billingclient:billing-ktx:$billingVersion")
}
```

整个支付流程可以总结为以下几点：

1. 通过 BillingClient 和 Google Play 建立连接，同时绑定用于回调支付结果的 PurchasesUpdatedListener 接口
2. 通过 BillingClient 查询到本地化处理的商品信息，也即 ProductDetails，从而拿到 **商品描述、基础方案、价格信息、优惠策略** 等属性
3. 根据查到的 ProductDetails，向 BillingClient 发起支付请求，调起支付弹窗
4. 在 PurchasesUpdatedListener 里拿到支付结果，判断用户的支付状态
5. 当确定用户支付成功后，根据商品类型择机对商品进行 **确认** 或 **消耗**

## BillingClient

BillingClient 是 Google Play 结算库与 App 进行通信的主接口，App 在执行任何与支付相关的操作之前，都需要先通过 BillingClient 和 Google Play 建立连接。在初始化 BillingClient 实例时，需要同时绑定 PurchasesUpdatedListener，以便得到支付结果的回调通知。也正因为如此，App 在同一时间段最多只能保持一个活跃的 BillingClient 连接，以免同一个支付事件同时回调多个 PurchasesUpdatedListener

```kotlin
private val purchasesUpdatedListener =
    PurchasesUpdatedListener { billingResult: BillingResult, purchases: List<Purchase>? ->

    }

private lateinit var billingClient: BillingClient

suspend fun startConnection(context: Context) {
    billingClient = buildBillingClient(context = context, purchasesUpdatedListener)
    startConnection(billingClient = mBillingClient)
}

private fun buildBillingClient(
    context: Context,
    listener: PurchasesUpdatedListener
): BillingClient {
    return BillingClient.newBuilder(context)
        .setListener(listener)
        .enablePendingPurchases()
        .build()
}

private suspend fun startConnection(billingClient: BillingClient): BillingResult? {
    return withContext(context = Dispatchers.Default) {
        if (billingClient.isReady) {
            return@withContext null
        }
        return@withContext suspendCancellableCoroutine { continuation ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (!continuation.isCompleted) {
                        continuation.resume(value = billingResult)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    if (!continuation.isCompleted) {
                        continuation.resume(value = null)
                    }
                }
            })
        }
    }
}
```

## ProductDetails

ProductDetails 也即商品详情，不管是一次性商品还是订阅型商品，都通过 ProductDetails 来承载具体的商品信息

查询 ProductDetails 需要两个查询参数：ProductId 和 商品类型，商品类型也即 **一次性商品 INAPP** 和 **订阅型商品 SUBS** 两种

```kotlin
private suspend fun queryProductDetails() {
    //查询一次性商品
    queryProductDetails(
        billingClient = mBillingClient,
        productIdList = setOf("1", "2"),
        productType = BillingClient.ProductType.INAPP
    )
    //查询订阅型商品
    queryProductDetails(
        billingClient = mBillingClient,
        productIdList = setOf("1", "2"),
        productType = BillingClient.ProductType.SUBS
    )
}

private suspend fun queryProductDetails(
    billingClient: BillingClient,
    productIdList: Set<String>,
    productType: String
): List<ProductDetails>? {
    return withContext(context = Dispatchers.Default) {
        if (!billingClient.isReady || productIdList.isEmpty()) {
            return@withContext null
        }
        val productDetailParamsList = productIdList.map {
            QueryProductDetailsParams
                .Product
                .newBuilder()
                .setProductId(it)
                .setProductType(productType)
                .build()
        }
        val queryProductDetailsParams = QueryProductDetailsParams
            .newBuilder()
            .setProductList(productDetailParamsList)
            .build()
        val productDetailsResult = billingClient.queryProductDetails(queryProductDetailsParams)
        productDetailsResult.productDetailsList
    }
}
```

ProductDetails 的数据结构如下所示，我们可以依靠这些信息来向用户展示商品详情。oneTimePurchaseOfferDetails 和 subscriptionOfferDetails 这两个字段就分别用来承载一次性商品和订阅型商品的价格信息

```json
{
	"productId": "",
	"productType": "",
	"title": "",
	"name": "",
	"description": "",
	"oneTimePurchaseOfferDetails": {},
	"subscriptionOfferDetails": []
}
```

## oneTimePurchaseOfferDetails

oneTimePurchaseOfferDetails 对应的是一次性商品的详情，数据结构比较简单，主要就是价格信息了

```json
{
	"priceAmountMicros": 548000000,
	"priceCurrencyCode": "HKD",
	"formattedPrice": "HK$548.00"
}
```

需要注意，Google Play 返回的价格信息都是做了本地化处理的，会自动根据当前设备的 Google Play 账号所对应的国家地区来返回详情，所以商品的价格货币代号 `priceCurrencyCode` 和格式化好的商品价格 `formattedPrice` 都会因实际情况而变化

## subscriptionOfferDetails

subscriptionOfferDetails 对应的是订阅型商品的详情

由于订阅型商品是可以包含多个 BasePlan 的，每个 BasePlan 又可以包含多个 Offer，所以 subscriptionOfferDetails 字段在 ProductDetails 中对应的数据类型是 `List<SubscriptionOfferDetails>`。每个 SubscriptionOfferDetails 都对应一个 Offer，每个 Offer 又关联一个 BasePlan，Google Play 以 Offer 为单位来返回价格信息

```json
[
    {
        "basePlanId": "yearly",
        "offerId": null,
        "offerToken": "xxx",
        "pricingPhases": {
            "pricingPhaseList": [
                {
                    "formattedPrice": "HK$469.00",
                    "priceAmountMicros": 469000000,
                    "priceCurrencyCode": "HKD",
                    "billingPeriod": "P1Y",
                    "billingCycleCount": 0,
                    "recurrenceMode": 1
                }
            ]
        }
    },
    {
        "basePlanId": "yearly",
        "offerId": "xxx",
        "offerToken": "xxx",
        "pricingPhases": {
            "pricingPhaseList": [
                {
                    "formattedPrice": "免費",
                    "priceAmountMicros": 0,
                    "priceCurrencyCode": "HKD",
                    "billingPeriod": "P1W",
                    "billingCycleCount": 1,
                    "recurrenceMode": 2
                },
                {
                    "formattedPrice": "HK$469.00",
                    "priceAmountMicros": 469000000,
                    "priceCurrencyCode": "HKD",
                    "billingPeriod": "P1Y",
                    "billingCycleCount": 0,
                    "recurrenceMode": 1
                }
            ]
        }
    }
]
```

上文有讲到，Offer 是包含价格阶段 PricingPhases 这个概念的，这个概念就体现在以上 Json 中，当中就可以解读出以下商品信息：

- 该商品包含一个 Id 为 yearly 的 basePlan，一共包含两个 Offer
- offerToken 用于唯一标识每一个 Offer，具有唯一性
- billingPeriod 用于表示计费周期，以 ISO 8601 格式来指定。例如，P1W 表示一周，P1Y 表示一年，P1M3D 表示一个月加三天
- billingCycleCount 用于表示计费周期的周期数。例如，以上的第二个 Offer 的第一个 PricingPhases，就表示允许用户免费试用一周；假如 billingCycleCount 是 2，就表示允许用户免费试用两周
- recurrenceMode 用于表示价格阶段的重复模式，当值为 1 或 3 时，billingCycleCount 值都会是 0 
   - 值为 1 就表示将在无限的计费周期内重复进行，除非用户主动取消
   - 值为 2 就表示将在 billingCycleCount 指定的周期内重复扣费
   - 值为 3 表示是一次性收费，不会重复
- 第一个 Offer 的 offerId 为 null，说明此 Offer 不包含实际的优惠策略，代表的其实是 BasePlan 的原价，所以 pricingPhaseList 也会只有一个值。且由于 billingPeriod 是 P1Y，说明关联的 BasePlan 的付费周期是一年。选中此 Offer 后用户就需要直接付 HK$469.00 的原价来进行订阅
- 第二个 Offer 的 offerId 不为 null，说明此 Offer 包含真实的优惠策略，所以 pricingPhaseList 的大小就会大于一。该 Offer 允许用户先免费试用一周，然后再和第一个 Offer 同样的价格和周期来进行订阅

所以说，想要解读出 BasePlan 的定价策略和 Offer 的优惠策略，就需要结合所有字段来进行解析。首先，不管我们在创建 BasePlan 时有没有为其指定优惠策略，Google Play 都会将 BasePlan 的原价视为一个 Offer 并返回，这种情况下 Offer 也只会有一个定价阶段。而对于真实的优惠策略，其 offerId 是必须设定的，自然也就不会为 null，也会有最多三个定价阶段。我们要区分出 “虚假的” Offer 和 "真实的” Offer。然后，再通过 pricingPhases 来解析出 BasePlan 的订阅周期和价格、Offer 的优惠策略、Offer 的价格阶段具体是如何设定的。这样我们才能向用户完整展示整个商品的价格信息

## launchBillingFlow

launchBillingFlow 用于调起支付弹窗发起支付操作，根据商品类型，其调用方式分为两种

假如要购买的是一次性商品，支付参数仅需要 ProductDetails 即可

```kotlin
private suspend fun launchBilling(
    activity: Activity,
    billingClient: BillingClient,
    productDetails: ProductDetails
): BillingResult {
    return withContext(context = Dispatchers.Main.immediate) {
        val productDetailsParams = BillingFlowParams
            .ProductDetailsParams
            .newBuilder()
            .setProductDetails(productDetails)
            .build()
        val billingFlowParams = BillingFlowParams
            .newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()
        billingClient.launchBillingFlow(activity, billingFlowParams)
    }
}
```

假如要购买的是订阅型商品，则需要同时传递 ProductDetails 和 offerToken

由于一个订阅型商品可能同时包含多个 BasePlan 和多个 Offer，每个 Offer 的优惠策略又各不相同。因此 App 在发起支付操作时，就需要通过 offerToken 来标明用户想要购买的到底是哪个 BasePlan，选中的又是哪个 Offer。而由于 Google Play 也会将 BasePlan 的原价视为一个 Offer 并返回，所以我们是可以自主选择要不要让用户享用优惠的，自由度还是比较高的

```kotlin
private suspend fun launchBilling(
    activity: Activity,
    billingClient: BillingClient,
    productDetails: ProductDetails,
    offerToken: String
): BillingResult {
    return withContext(context = Dispatchers.Main.immediate) {
        val productDetailsParams = BillingFlowParams
            .ProductDetailsParams
            .newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offerToken)
            .build()
        val billingFlowParams = BillingFlowParams
            .newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()
        billingClient.launchBillingFlow(activity, billingFlowParams)
    }
}
```

之后，我们在 PurchasesUpdatedListener 回调里来获取用户的支付结果

假如用户已支付成功，Purchase 就包含了此笔订单的具体信息，包括 ProductId、OrderId、Quantity、PurchaseTime 等

```kotlin
private val purchasesUpdatedListener =
    PurchasesUpdatedListener { billingResult: BillingResult, purchases: List<Purchase>? ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (!purchases.isNullOrEmpty()) {
                    purchases.forEach {
                        when (it.purchaseState) {
                            Purchase.PurchaseState.PURCHASED -> {
                                //用户支付成功
                            }

                            Purchase.PurchaseState.PENDING -> {
                                //用户仅是预创建了订单，还未真正付款
                            }

                            Purchase.PurchaseState.UNSPECIFIED_STATE -> {
                                //未知
                            }
                        }
                    }
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                //用户取消支付
            }

            else -> {

            }
        }
    }
```

## acknowledgePurchase

用户支付成功后，就需要对订单进行确认了，否则 Google Play 会在限定时间内退款给用户

```kotlin
private suspend fun acknowledgePurchase(
    billingClient: BillingClient,
    purchase: Purchase
): Boolean {
    return withContext(context = Dispatchers.Default) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            return@withContext false
        }
        if (purchase.isAcknowledged) {
            return@withContext true
        }
        if (!billingClient.isReady) {
            return@withContext false
        }
        val acknowledgePurchaseParams = AcknowledgePurchaseParams
            .newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        val acknowledgePurchase = billingClient.acknowledgePurchase(acknowledgePurchaseParams)
        acknowledgePurchase.responseCode == BillingClient.BillingResponseCode.OK
    }
}
```

## consumePurchase

如果用户购买的是消耗型的一次性商品，那么就需要根据实际业务择机对订单执行消耗操作了

```kotlin
private suspend fun consumePurchase(
    billingClient: BillingClient,
    purchase: Purchase
): Boolean {
    return withContext(context = Dispatchers.Default) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            return@withContext false
        }
        if (!billingClient.isReady) {
            return@withContext false
        }
        val consumeParams = ConsumeParams
            .newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        val consumeResult = billingClient.consumePurchase(consumeParams)
        consumeResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK
    }
}
```

# 五、鉴权

当用户购买商品后，就需要来考虑如何对用户进行鉴权了。如果鉴权失败或者是鉴权错了，不仅会给用户带来不良体验，引来用户投诉，也有可能会给项目带来不可估量的资金损失

按照一般情况，App 在供用户使用时，App 都会为当前用户创建一个自己账户体系下的用户身份，我们可以称之为 appUser。当用户购买商品后，这笔订单也会和当前设备付款的 Google Play 账号绑定在一起，我们可以称之为 gpUser

如此一来，这笔订单就会和两个不同角度下的用户产生关联。这也就连锁带来一个问题：商品代表的权益应该挂载在哪个用户的名下？appUser 还是 gpUser ？

这两个选择都各有优缺点

挂载在 appUser 名下：

- 优点：用户权益清晰明确，可以精准隔离用户的权益状态
- 缺点：在国外，以游客身份来购买虚拟商品是很常见的情况，假如 App 只允许正式用户（绑定了邮箱或者电话号码）才能购买商品的话，很有可能会流失大部分的潜在付费用户。因此，如果 appUser 是游客的话，当用户卸载应用、更换或者重置设备后，就有可能导致已付费的用户再也找不回这笔订单了

挂载在 gpUser 名下：

- 优点：即使用户卸载应用、更换或者重置设备，只要当前设备登录的就是付款时的 Google Play 账号，App 都能通过 Billing SDK 的 `queryPurchasesAsync` 方法重新找回该账号名下所有的订单信息，不用担心出现权益丢失的情况。同个 Google Play 账号在不同设备上也能共同享有 App 的权益，用户体验是最好的
- 缺点：App 是无法拿到 gpUser 的唯一身份标识的，容易出现账号倒卖的情况，多个用户通过共享同一个 Google Play 账号来一起享有同一笔订单的权益

所以说，App 需要根据自己的业务类型和用户属性，来决定是否要允许游客也能进行购买操作，用户应该以哪种维度来进行身份鉴权，当发现同笔订单在多台设备上生效时，又应该如何避免资产损失

# 六、最后

本文主要是以移动端的角度来进行阐述，虽然 Google Play 结算系统也允许在没有 App 后端服务参与的情况下就直接完成整个支付流程并完成用户鉴权，但为了安全性考虑，最好还是需要将订单信息同步保存到服务端，并由服务端对订单进行校验后再决定是否要下发权益。此外，用户是可以在不经过 App 的情况下，直接从 Google Play 中取消订阅或者恢复订阅，App 无法实时获知该笔订单的状态变化，此时 Google Play 也只会通过 **开发者实时通知** 将这种变化通知给服务端，这种情况下也需要服务端的参与才能完整记录下用户的整个付费状态变化
