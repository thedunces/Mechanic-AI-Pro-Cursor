import {getFirestore} from "firebase-admin/firestore";
import {onMessagePublished} from "firebase-functions/v2/pubsub";
import {
  PACKAGE_NAME,
  PRO_PRODUCT_ID,
  tokenHash,
  writeEntitlementFromPurchase,
} from "./subscription";

interface PlayRtdnMessage {
  version?: string;
  packageName?: string;
  eventTimeMillis?: string;
  subscriptionNotification?: {
    version?: string;
    notificationType?: number;
    purchaseToken?: string;
    subscriptionId?: string;
  };
  voidedPurchaseNotification?: {
    purchaseToken?: string;
    orderId?: string;
    productType?: number;
    refundType?: number;
  };
}

/**
 * Handles Google Play Real-time Developer Notifications.
 * Create a Pub/Sub topic named play-rtdn and point Play Console RTDN at it.
 */
export const playRtdn = onMessagePublished(
  {
    topic: "play-rtdn",
    maxInstances: 10,
    memory: "256MiB",
  },
  async (event) => {
    const message = event.data.message.json as PlayRtdnMessage | undefined;
    if (!message || message.packageName !== PACKAGE_NAME) {
      console.warn("Ignoring RTDN for unexpected package", message?.packageName);
      return;
    }

    const purchaseToken = message.subscriptionNotification?.purchaseToken ??
      message.voidedPurchaseNotification?.purchaseToken;
    const productId = message.subscriptionNotification?.subscriptionId ?? PRO_PRODUCT_ID;
    if (!purchaseToken) {
      console.warn("RTDN missing purchase token", message);
      return;
    }

    const lookup = await getFirestore()
      .doc(`purchaseLookups/${tokenHash(purchaseToken)}`)
      .get();
    const uid = lookup.data()?.uid as string | undefined;
    if (!uid) {
      console.warn("No user mapped for Play purchase token hash");
      return;
    }

    try {
      await writeEntitlementFromPurchase(uid, purchaseToken, productId, false);
    } catch (error) {
      console.error("Failed to sync entitlement from Play RTDN", error);
    }
  },
);
