const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTimestamp() {
  return new Date(Date.now() - 2000);
}

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

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("consultations").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("reminders").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("orders").get());
});

test("User profile: owner can create and update allowed fields, rejects shadow fields and cross-user read", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceRef = aliceDb.collection("users").doc(ALICE_UID);
  const ts = pastTimestamp();

  await assertSucceeds(aliceRef.set({
    userId: ALICE_UID,
    displayName: "Nguyen Van A",
    email: "a@example.com",
    phoneNumber: "0912345678",
    notifyOnReply: true,
    createdAt: ts,
    updatedAt: ts,
  }));

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());

  await assertFails(aliceRef.update({
    displayName: "Updated",
    isAdmin: true,
    updatedAt: pastTimestamp(),
  }));

  await assertSucceeds(aliceRef.update({
    phoneNumber: "0987654321",
    updatedAt: pastTimestamp(),
  }));
});

test("Consultations & messages: owner can create session + messages, cross-user denied, resolved session locked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const sessionRef = aliceDb.collection("users").doc(ALICE_UID).collection("consultations").doc("session_1");
  const ts = pastTimestamp();

  await assertSucceeds(sessionRef.set({
    userId: ALICE_UID,
    title: "Tranh chấp đất đai",
    category: "land",
    lastMessagePreview: "Xin hỏi thủ tục hòa giải cơ sở...",
    messageCount: 1,
    status: "active",
    createdAt: ts,
    updatedAt: ts,
  }));

  const msgRef = sessionRef.collection("messages").doc("msg_1");
  await assertSucceeds(msgRef.set({
    userId: ALICE_UID,
    sender: "user",
    content: "Xin hỏi thủ tục hòa giải tranh chấp đất đai theo Luật Đất đai 2024?",
    createdAt: pastTimestamp(),
  }));

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("consultations").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("consultations").doc("session_1").collection("messages").get());

  await assertSucceeds(sessionRef.update({
    status: "resolved",
    updatedAt: pastTimestamp(),
  }));

  await assertFails(sessionRef.update({
    title: "Changed after resolved",
    updatedAt: pastTimestamp(),
  }));
});

test("Bookmarks & reminders: owner CRUD succeeds, cross-user fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const bmRef = aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks").doc("bm_1");
  await assertSucceeds(bmRef.set({
    userId: ALICE_UID,
    documentId: "ldd_2024",
    articleCode: "Điều 235 Luật Đất đai 2024",
    title: "Hòa giải tranh chấp đất đai",
    category: "land",
    createdAt: pastTimestamp(),
  }));

  const remRef = aliceDb.collection("users").doc(ALICE_UID).collection("reminders").doc("rem_1");
  const ts = pastTimestamp();
  await assertSucceeds(remRef.set({
    userId: ALICE_UID,
    title: "Nộp đơn khiếu nại THADS",
    category: "enforcement",
    dueDateMillis: Date.now() + 86400000,
    isCompleted: false,
    notifyEnabled: true,
    createdAt: ts,
    updatedAt: ts,
  }));

  await assertSucceeds(remRef.update({
    isCompleted: true,
    updatedAt: pastTimestamp(),
  }));

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("reminders").doc("rem_1").delete());
});

test("Orders: enforces pending_transfer on create, blocks amount tampering and terminal status edits", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const orderRef = aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("ord_1");
  const ts = pastTimestamp();

  await assertFails(orderRef.set({
    userId: ALICE_UID,
    packageId: "pkg_deep_30",
    packageTitle: "Tư vấn chuyên sâu 1-1",
    amountVnd: 500000,
    paymentMethod: "agribank_vietqr",
    transferCode: "MGL1001",
    contactPhone: "0915855111",
    caseSummary: "Cần rà soát hồ sơ thi hành án",
    status: "confirmed",
    createdAt: ts,
    updatedAt: ts,
  }));

  await assertSucceeds(orderRef.set({
    userId: ALICE_UID,
    packageId: "pkg_deep_30",
    packageTitle: "Tư vấn chuyên sâu 1-1",
    amountVnd: 500000,
    paymentMethod: "agribank_vietqr",
    transferCode: "MGL1001",
    contactPhone: "0915855111",
    caseSummary: "Cần rà soát hồ sơ thi hành án",
    status: "pending_transfer",
    createdAt: ts,
    updatedAt: ts,
  }));

  await assertFails(orderRef.update({
    amountVnd: 1000,
    status: "submitted_receipt",
    updatedAt: pastTimestamp(),
  }));

  await assertSucceeds(orderRef.update({
    status: "submitted_receipt",
    updatedAt: pastTimestamp(),
  }));

  await assertFails(orderRef.delete());
});
