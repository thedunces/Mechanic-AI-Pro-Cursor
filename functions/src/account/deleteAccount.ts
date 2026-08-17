import {getAuth} from "firebase-admin/auth";
import {getFirestore} from "firebase-admin/firestore";
import {HttpsError, onCall} from "firebase-functions/v2/https";

const USER_COLLECTIONS = ["vehicles", "sessions", "entitlements", "usage"];

async function deleteCollection(path: string): Promise<void> {
  const db = getFirestore();
  const documents = await db.collection(path).listDocuments();
  while (documents.length > 0) {
    const batch = db.batch();
    const chunk = documents.splice(0, 400);
    chunk.forEach((doc) => batch.delete(doc));
    await batch.commit();
  }
}

export const deleteAccount = onCall(
  {
    enforceAppCheck: true,
    maxInstances: 5,
    memory: "256MiB",
  },
  async (request) => {
    const uid = request.auth?.uid;
    if (!uid) {
      throw new HttpsError("unauthenticated", "Sign in before deleting your account.");
    }

    const db = getFirestore();
    for (const collectionName of USER_COLLECTIONS) {
      await deleteCollection(`users/${uid}/${collectionName}`);
    }

    const lookups = await db.collection("purchaseLookups").where("uid", "==", uid).get();
    if (!lookups.empty) {
      const batch = db.batch();
      lookups.docs.forEach((doc) => batch.delete(doc.ref));
      await batch.commit();
    }

    await db.doc(`users/${uid}`).delete().catch(() => undefined);
    await getAuth().deleteUser(uid);

    return {deleted: true};
  },
);
