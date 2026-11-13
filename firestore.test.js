const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_store_1";
const BOB_UID = "bob_store_2";
const safePastDate = () => new Date(Date.now() - 1000);

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot list or write categories, products, or sales", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/categories`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/products`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/sales`).get());
  await assertFails(
    unauthDb.doc(`users/${ALICE_UID}/categories/cat_1`).set({
      userId: ALICE_UID,
      name: "Mode",
      colorHex: "#1565C0",
      iconKey: "checkroom",
      createdAt: safePastDate(),
      updatedAt: safePastDate(),
    })
  );
});

test("Cross-user isolation: Bob cannot read or write Alice's store data", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.doc(`users/${ALICE_UID}/categories/cat_1`).set({
      userId: ALICE_UID,
      name: "Accessoires",
      colorHex: "#1565C0",
      iconKey: "watch",
      createdAt: safePastDate(),
      updatedAt: safePastDate(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection(`users/${ALICE_UID}/categories`).get());
  await assertFails(bobDb.doc(`users/${ALICE_UID}/categories/cat_1`).get());
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/categories/cat_2`).set({
      userId: BOB_UID,
      name: "Hack",
      colorHex: "#000000",
      iconKey: "tag",
      createdAt: safePastDate(),
      updatedAt: safePastDate(),
    })
  );
});

test("Owner can create category and product referencing existing category, and update stock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/categories/cat_shoes`).set({
      userId: ALICE_UID,
      name: "Chaussures",
      colorHex: "#1565C0",
      iconKey: "store",
      createdAt: safePastDate(),
      updatedAt: safePastDate(),
    })
  );

  // Orphaned product pointing to missing category must fail
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/products/prod_orphan`).set({
      userId: ALICE_UID,
      categoryId: "missing_cat",
      categoryName: "Inconnue",
      name: "Baskets",
      barcode: "3700123456789",
      price: 59.9,
      stock: 12,
      minStockAlert: 3,
      createdAt: safePastDate(),
      updatedAt: safePastDate(),
    })
  );

  // Valid product referencing existing category succeeds
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/products/prod_1`).set({
      userId: ALICE_UID,
      categoryId: "cat_shoes",
      categoryName: "Chaussures",
      name: "Sneakers Bleu Azur",
      barcode: "3700123456789",
      price: 79.5,
      stock: 15,
      minStockAlert: 4,
      createdAt: safePastDate(),
      updatedAt: safePastDate(),
    })
  );

  // Shadow update with ghost field must fail
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/products/prod_1`).update({
      stock: 10,
      isAdmin: true,
      updatedAt: safePastDate(),
    })
  );

  // Valid stock decrement succeeds
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/products/prod_1`).update({
      stock: 14,
      updatedAt: safePastDate(),
    })
  );
});

test("Owner can create immutable paid sale, cannot modify or delete it", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/sales/sale_001`).set({
      userId: ALICE_UID,
      receiptNumber: "TK-2026-001",
      totalAmount: 79.5,
      itemCount: 1,
      paymentMethod: "card",
      status: "paid",
      items: [
        {
          productId: "prod_1",
          productName: "Sneakers Bleu Azur",
          barcode: "3700123456789",
          categoryName: "Chaussures",
          unitPrice: 79.5,
          quantity: 1,
          lineTotal: 79.5,
        },
      ],
      createdAt: safePastDate(),
      updatedAt: safePastDate(),
    })
  );

  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/sales/sale_001`).update({
      totalAmount: 0,
      updatedAt: safePastDate(),
    })
  );
  await assertFails(aliceDb.doc(`users/${ALICE_UID}/sales/sale_001`).delete());
});
