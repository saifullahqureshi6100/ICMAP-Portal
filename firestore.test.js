const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_student_1";
const BOB_UID = "bob_student_2";

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

test("Unauthenticated user: denied across all user collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 2000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("studyMaterials").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("studyPlans").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("quizAttempts").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      studyGoal: "Ace Exams",
      dailyTargetMinutes: 45,
      totalStudyMinutes: 0,
      completedSessions: 0,
      createdAt: past,
      updatedAt: past,
    })
  );
});

test("UserProfile: owner create, partial update, immutable guards, and cross-user denial", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Scholar",
      studyGoal: "Complete Drive Syllabus",
      dailyTargetMinutes: 60,
      totalStudyMinutes: 0,
      completedSessions: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      totalStudyMinutes: 30,
      completedSessions: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt modification
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject future timestamp tampering
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      totalStudyMinutes: 90,
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Reject shadow ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cross-user isolation
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("StudyMaterials & MaterialProgress: owner CRUD, list, and validation bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("studyMaterials")
      .doc("mat_custom_1")
      .set({
        userId: ALICE_UID,
        title: "Lecture Notes Ch 4",
        description: "Summary of chapter 4 concepts",
        materialType: "READING",
        sourceUrl: "https://drive.google.com/file/d/1C976hWaHp0S8gjrjMIe5M64p1IkmJ1JW/view",
        contentMarkdown: "# Chapter 4\nKey points here.",
        category: "Core Module",
        estimatedMinutes: 25,
        createdAt: created,
        updatedAt: created,
      })
  );

  // List subcollection succeeds for owner (even when querying empty or populated)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("studyMaterials").get()
  );

  // Reject injecting @DocumentId `id` field
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("studyMaterials")
      .doc("mat_bad_id")
      .set({
        id: "mat_bad_id",
        userId: ALICE_UID,
        title: "Bad ID payload",
        description: "",
        materialType: "READING",
        sourceUrl: "",
        contentMarkdown: "",
        category: "General",
        estimatedMinutes: 10,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Reject invalid materialType enum
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("studyMaterials")
      .doc("mat_bad_enum")
      .set({
        userId: ALICE_UID,
        title: "Bad Enum",
        description: "",
        materialType: "EXECUTABLE_BIN",
        sourceUrl: "",
        contentMarkdown: "",
        category: "General",
        estimatedMinutes: 10,
        createdAt: created,
        updatedAt: created,
      })
  );

  // MaterialProgress create & update
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("materialProgress")
      .doc("drive_doc_6")
      .set({
        userId: ALICE_UID,
        materialId: "drive_doc_6",
        completed: true,
        personalNotes: "Reviewed key definitions.",
        minutesSpent: 20,
        createdAt: created,
        updatedAt: created,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("materialProgress")
      .doc("drive_doc_6")
      .update({
        personalNotes: "Reviewed definitions and solved examples.",
        minutesSpent: 35,
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Cross-user list/read denied
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("studyMaterials").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("materialProgress").get()
  );
});

test("StudyPlans & QuizAttempts: owner workflows and strict bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("studyPlans")
      .doc("plan_1")
      .set({
        userId: ALICE_UID,
        title: "Watch Lecture Clip & Take 5 MCQs",
        topic: "Module 1",
        dayLabel: "Today",
        targetMinutes: 30,
        completed: false,
        linkedMaterialId: "drive_file_3",
        createdAt: created,
        updatedAt: created,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("studyPlans")
      .doc("plan_1")
      .update({
        completed: true,
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("quizAttempts")
      .doc("quiz_1")
      .set({
        userId: ALICE_UID,
        topic: "Core Concepts Quiz",
        totalQuestions: 5,
        correctCount: 4,
        durationSeconds: 125,
        fullScreenUsed: true,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Reject impossible score (correctCount > totalQuestions)
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("quizAttempts")
      .doc("quiz_bad")
      .set({
        userId: ALICE_UID,
        topic: "Cheated Quiz",
        totalQuestions: 5,
        correctCount: 8,
        durationSeconds: 10,
        fullScreenUsed: true,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Reject immutable quiz attempt modification
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("quizAttempts")
      .doc("quiz_1")
      .update({
        correctCount: 5,
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Cross-user isolation
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("studyPlans").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("quizAttempts").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("flashcards").get()
  );
});

test("Flashcards: owner create, mastery update, delete, immutable guards, and cross-user denial", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("flashcards")
      .doc("fc_1")
      .set({
        userId: ALICE_UID,
        materialId: "om_study_text_2026",
        deckTitle: "M3 Organizational Management",
        front: "How is Overall Equipment Effectiveness (OEE) calculated?",
        back: "OEE = Availability x Performance x Quality.",
        hint: "Multiply three operational ratios.",
        mastered: false,
        reviewCount: 0,
        createdAt: created,
        updatedAt: created,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("flashcards")
      .doc("fc_1")
      .update({
        mastered: true,
        reviewCount: 1,
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Reject mutating front/back on review update
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("flashcards")
      .doc("fc_1")
      .update({
        front: "Tampered question",
        updatedAt: new Date(Date.now() - 500),
      })
  );

  // Reject empty front prompt
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("flashcards")
      .doc("fc_bad")
      .set({
        userId: ALICE_UID,
        materialId: "",
        deckTitle: "Deck",
        front: "",
        back: "Answer",
        hint: "",
        mastered: false,
        reviewCount: 0,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Cross-user write denied
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("flashcards")
      .doc("fc_1")
      .delete()
  );

  // Owner delete succeeds
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("flashcards")
      .doc("fc_1")
      .delete()
  );
});
