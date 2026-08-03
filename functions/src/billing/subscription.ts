import {createHash} from "node:crypto";
import {getFirestore, Timestamp} from "firebase-admin/firestore";
import {google} from "googleapis";
import {HttpsError, onCall} from "firebase-functions/v2/https";

const PACKAGE_NAME = "com.mechanicai.pro";
const PRO_PRODUCT_ID = "mechanic_ai_pro_monthly";
const PRO_MONTHLY_LIMIT = 100;
const FREE_MONTHLY_LIMIT = 3;

type Tier = "free" | "pro";

export interface Entitlement {
  tier: Tier;
  monthlyLimit: number;
  productId: string | null;
  expiresAt: Timestamp | null;
  autoRenewing: boolean;
  updatedAt: Timestamp;
}

function accountHash(uid: string): string {
  return createHash("sha256").update(uid).digest("hex");
}

function isActiveState(state?: string | null): boolean {
  return state === "SUBSCRIPTION_STATE_ACTIVE" ||
    state === "SUBSCRIPTION_STATE_IN_GRACE_PERIOD";
}

export const verifySubscription = onCall(
  {
    enforceAppCheck: true,
    maxInstances: 10,
    memory: "256MiB",
  },
  async (request) => {
    const uid = request.auth?.uid;
    if (!uid) {
      throw new HttpsError("unauthenticated", "Sign in before restoring purchases.");
    }

    const purchaseToken = String(request.data?.purchaseToken ?? "");
    const productId = String(request.data?.productId ?? "");
    if (!purchaseToken || productId !== PRO_PRODUCT_ID) {
      throw new HttpsError("invalid-argument", "Invalid subscription purchase.");
    }

    const auth = new google.auth.GoogleAuth({
      scopes: ["https://www.googleapis.com/auth/androidpublisher"],
    });
    const publisher = google.androidpublisher({version: "v3", auth});

    let purchase;
    try {
      const response = await publisher.purchases.subscriptionsv2.get({
        packageName: PACKAGE_NAME,
        token: purchaseToken,
      });
      purchase = response.data;
    } catch (error) {
      console.error("Play subscription verification failed", error);
      throw new HttpsError("failed-precondition", "Google Play could not verify this purchase.");
    }

    const purchasedProducts = purchase.lineItems
      ?.map((item) => item.productId)
      .filter(Boolean) ?? [];
    if (!purchasedProducts.includes(PRO_PRODUCT_ID)) {
      throw new HttpsError("permission-denied", "Purchase is not for Mechanic AI Pro.");
    }

    const expectedAccountId = accountHash(uid);
    const actualAccountId = purchase.externalAccountIdentifiers?.obfuscatedExternalAccountId;
    if (actualAccountId !== expectedAccountId) {
      throw new HttpsError("permission-denied", "Purchase belongs to another account.");
    }

    const active = isActiveState(purchase.subscriptionState);
    const expiry = purchase.lineItems
      ?.map((item) => item.expiryTime)
      .filter((value): value is string => Boolean(value))
      .sort()
      .at(-1);
    const autoRenewing = purchase.lineItems?.some(
      (item) => item.autoRenewingPlan?.autoRenewEnabled === true,
    ) ?? false;

    const entitlement: Entitlement = {
      tier: active ? "pro" : "free",
      monthlyLimit: active ? PRO_MONTHLY_LIMIT : FREE_MONTHLY_LIMIT,
      productId: active ? PRO_PRODUCT_ID : null,
      expiresAt: expiry ? Timestamp.fromDate(new Date(expiry)) : null,
      autoRenewing,
      updatedAt: Timestamp.now(),
    };

    await getFirestore()
      .doc(`users/${uid}/entitlements/current`)
      .set(entitlement, {merge: true});

    return {
      tier: entitlement.tier,
      monthlyLimit: entitlement.monthlyLimit,
      expiresAt: expiry ?? null,
      autoRenewing,
    };
  },
);

export async function getEffectiveEntitlement(uid: string): Promise<Entitlement> {
  const ref = getFirestore().doc(`users/${uid}/entitlements/current`);
  const snapshot = await ref.get();
  const stored = snapshot.data() as Partial<Entitlement> | undefined;
  const now = Timestamp.now();
  const proIsCurrent = stored?.tier === "pro" &&
    stored.expiresAt instanceof Timestamp &&
    stored.expiresAt.toMillis() > now.toMillis();

  return {
    tier: proIsCurrent ? "pro" : "free",
    monthlyLimit: proIsCurrent ? PRO_MONTHLY_LIMIT : FREE_MONTHLY_LIMIT,
    productId: proIsCurrent ? PRO_PRODUCT_ID : null,
    expiresAt: proIsCurrent ? stored?.expiresAt ?? null : null,
    autoRenewing: proIsCurrent && stored?.autoRenewing === true,
    updatedAt: stored?.updatedAt instanceof Timestamp ? stored.updatedAt : now,
  };
}
