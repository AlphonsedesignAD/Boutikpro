const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const { initializeApp } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const bcrypt = require("bcryptjs");

initializeApp();

const db = getFirestore();
const adminBootstrapPassword = defineSecret("ADMIN_BOOTSTRAP_PASSWORD");
const ADMIN_UID = "boutikpro-admin";

function cleanUsername(value) {
  return String(value || "").trim().toLowerCase();
}

function requireAdmin(request) {
  if (!request.auth || request.auth.token.role !== "admin") {
    throw new HttpsError("permission-denied", "Accès réservé à l'administrateur.");
  }
}

async function ensureAdminUser() {
  try {
    await getAuth().getUser(ADMIN_UID);
  } catch (error) {
    if (error.code !== "auth/user-not-found") throw error;
    await getAuth().createUser({
      uid: ADMIN_UID,
      displayName: "Administrateur BoutikPro",
      disabled: false
    });
  }
  await getAuth().setCustomUserClaims(ADMIN_UID, {
    role: "admin",
    storeOwnerId: ADMIN_UID
  });
}

exports.loginWithUsername = onCall(
  { secrets: [adminBootstrapPassword], enforceAppCheck: true },
  async (request) => {
    const username = cleanUsername(request.data && request.data.username);
    const password = String(request.data && request.data.password || "");

    if (!/^[a-z0-9._-]{3,30}$/.test(username) || password.length < 4 || password.length > 128) {
      throw new HttpsError("invalid-argument", "Identifiant ou mot de passe invalide.");
    }

    let uid;
    let role;
    let storeOwnerId;

    if (username === "admin") {
      const expected = adminBootstrapPassword.value();
      if (!expected || password !== expected) {
        throw new HttpsError("unauthenticated", "Identifiant ou mot de passe incorrect.");
      }
      await ensureAdminUser();
      uid = ADMIN_UID;
      role = "admin";
      storeOwnerId = ADMIN_UID;
    } else {
      const directory = await db.collection("accountDirectory").doc(username).get();
      if (!directory.exists) {
        throw new HttpsError("unauthenticated", "Identifiant ou mot de passe incorrect.");
      }
      uid = directory.get("uid");
      const account = await db.collection("accounts").doc(uid).get();
      if (!account.exists || account.get("active") !== true) {
        throw new HttpsError("unauthenticated", "Ce compte est désactivé.");
      }
      const valid = await bcrypt.compare(password, account.get("passwordHash") || "");
      if (!valid) {
        throw new HttpsError("unauthenticated", "Identifiant ou mot de passe incorrect.");
      }
      role = account.get("role") || "seller";
      storeOwnerId = account.get("storeOwnerId") || ADMIN_UID;
      await getAuth().setCustomUserClaims(uid, { role, storeOwnerId });
    }

    const token = await getAuth().createCustomToken(uid, { role, storeOwnerId });
    return { token, role, storeOwnerId, username };
  }
);

exports.createStaffAccount = onCall({ enforceAppCheck: true }, async (request) => {
  requireAdmin(request);
  const username = cleanUsername(request.data && request.data.username);
  const password = String(request.data && request.data.password || "");

  if (!/^[a-z0-9._-]{3,30}$/.test(username) || username === "admin") {
    throw new HttpsError("invalid-argument", "Choisissez un identifiant de 3 à 30 caractères.");
  }
  if (password.length < 4 || password.length > 128) {
    throw new HttpsError("invalid-argument", "Le mot de passe doit contenir au moins 4 caractères.");
  }

  const directoryRef = db.collection("accountDirectory").doc(username);
  const existing = await directoryRef.get();
  if (existing.exists) {
    throw new HttpsError("already-exists", "Cet identifiant est déjà utilisé.");
  }

  const user = await getAuth().createUser({ displayName: username, disabled: false });
  const passwordHash = await bcrypt.hash(password, 12);
  await getAuth().setCustomUserClaims(user.uid, {
    role: "seller",
    storeOwnerId: ADMIN_UID
  });

  const batch = db.batch();
  batch.set(directoryRef, { uid: user.uid, createdAt: FieldValue.serverTimestamp() });
  batch.set(db.collection("accounts").doc(user.uid), {
    uid: user.uid,
    username,
    passwordHash,
    role: "seller",
    active: true,
    storeOwnerId: ADMIN_UID,
    createdAt: FieldValue.serverTimestamp()
  });
  await batch.commit();

  return { uid: user.uid, username, role: "seller" };
});

exports.listStaffAccounts = onCall({ enforceAppCheck: true }, async (request) => {
  requireAdmin(request);
  const snapshot = await db.collection("accounts").orderBy("createdAt", "desc").get();
  return {
    accounts: snapshot.docs.map((doc) => ({
      uid: doc.id,
      username: doc.get("username"),
      active: doc.get("active") === true,
      role: doc.get("role") || "seller"
    }))
  };
});

exports.setStaffAccountActive = onCall({ enforceAppCheck: true }, async (request) => {
  requireAdmin(request);
  const uid = String(request.data && request.data.uid || "");
  const active = request.data && request.data.active;
  if (!uid || uid === ADMIN_UID || typeof active !== "boolean") {
    throw new HttpsError("invalid-argument", "Demande invalide.");
  }
  const accountRef = db.collection("accounts").doc(uid);
  const account = await accountRef.get();
  if (!account.exists) throw new HttpsError("not-found", "Compte introuvable.");
  await accountRef.update({ active, updatedAt: FieldValue.serverTimestamp() });
  await getAuth().updateUser(uid, { disabled: !active });
  return { success: true };
});
