import * as admin from "firebase-admin";
import {deleteAccount} from "./account/deleteAccount";
import {diagnose} from "./ai/productionDiagnose";
import {playRtdn} from "./billing/rtdn";
import {verifySubscription} from "./billing/subscription";

admin.initializeApp();

export {deleteAccount, diagnose, playRtdn, verifySubscription};
