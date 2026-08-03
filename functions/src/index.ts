import * as admin from "firebase-admin";
import {diagnose} from "./ai/productionDiagnose";
import {verifySubscription} from "./billing/subscription";

admin.initializeApp();

export {diagnose, verifySubscription};
