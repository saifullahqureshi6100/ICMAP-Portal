import {
  DailyBriefPlan,
  DailyRoutineSlot,
  FlashcardItem,
  McqQuestionItem,
  TypingQuestionItem
} from '../types';
import { OM_MCQ_QUESTIONS, TYPING_QUESTIONS } from './icmapData';

export function getDaysToDec5Exam(now = new Date()): number {
  const year = now.getFullYear();
  let exam = new Date(year, 11, 5, 9, 0, 0); // 5 December 2026 (month index 11)
  if (now.getTime() > exam.getTime() + 12 * 3600 * 1000) {
    exam = new Date(year + 1, 11, 5, 9, 0, 0);
  }
  const startToday = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  const startExam = new Date(exam.getFullYear(), exam.getMonth(), exam.getDate()).getTime();
  return Math.max(0, Math.round((startExam - startToday) / (24 * 3600 * 1000)));
}

const EMERGENCY_NINE_DAY_ROADMAP: Array<{
  topic: string;
  videoQuery: string;
  technique: string;
}> = [
  {
    topic: 'Day 1 Emergency Start: Balanced Scorecard, CSFs & Past Paper Q.4 + Q.7 (Units 13–14)',
    videoQuery: 'Balanced Scorecard Kaplan Norton ICMA OM Unit 14',
    technique:
      '80/20 Pareto Emergency Rule: From ZERO preparation, hit the 20% topics carrying 50% marks first! Anchor 4 BSC perspectives to 4 walls of your office room & memorize Q.4 (5 drawbacks).'
  },
  {
    topic: 'Day 2: Mintzberg 5 Parts, Enterprise PESTEL/ESG & Production Systems (Units 1–3 + Q.6)',
    videoQuery: 'Mintzberg 5 Organizational Parts Job Batch Flow Production',
    technique:
      'Visual Diagram Recall: Sketch Mintzberg’s 5 parts on 1 page in 60 seconds during office tea break — Strategic Apex, Middle Line, Operating Core, Technostructure, Support Staff.'
  },
  {
    topic: 'Day 3: Plant Maintenance, 5S, TPM OEE, MRP & OPT Drum-Buffer-Rope (Units 4–5 + Q.10)',
    videoQuery: 'TPM OEE Calculation MRP OPT Drum Buffer Rope Theory of Constraints',
    technique:
      'Formula & Acronym Lock: OEE = Availability × Performance × Quality; 5S = Sort, Set, Shine, Standardize, Sustain; OPT = Drum-Buffer-Rope + 5 MRP Benefits (Q.10).'
  },
  {
    topic: 'Day 4: Quality Circles, Juran COQ, Six Sigma & Half-Book Mock 1 Prep (Units 6–7 + Q.9)',
    videoQuery: 'Quality Circles Cost of Quality Six Sigma DMAIC ISO',
    technique:
      'Zero-to-Merit Check: Part A (Units 1–7) finishes tonight! Tomorrow is Half-Book Grand Test (30%–50% weight) — flag every <70% MCQ.'
  },
  {
    topic: 'Day 5: HRM Motivation (Herzberg, Handy Shamrock), Pfeffer 7 & Kolb (Units 8–9 + Q.2, Q.3, Q.8)',
    videoQuery: 'Herzberg Two Factor Handy Shamrock Pfeffer 7 Practices Kolb Cycle',
    technique:
      'Past Paper Cluster Lock: Q.2 (HRM External Factors), Q.3 (Agency Outsourcing), Q.8 (Halo Effect, Contagious Bias, Stereotyping) appear repeatedly in ICMA papers!'
  },
  {
    topic: 'Day 6: Marketing Concept vs Sales, 7Ps, BCG & Ansoff Matrix (Units 10–12 + Q.1, Q.5)',
    videoQuery: 'Marketing Concept Sales Orientation 7Ps Ansoff Matrix BCG',
    technique:
      'Compare & Contrast Table: Never confuse Sales Orientation (push stock, short-term) with Marketing Concept (target market satisfaction).'
  },
  {
    topic: 'Day 7: Weekly & Portion-Wide Grand Test + Building Block Model + Web3/AI Units Check',
    videoQuery: 'Fitzgerald Moon Building Block Model Performance Pyramid Lynch Cross',
    technique:
      'Active Recall Under Pressure: Type every 5-mark answer in 7 minutes max with numbered headings so you never run out of time on 5 December.'
  },
  {
    topic: 'Day 8 (Dec 3): Full-Course Merit Mock 1 & Mock 2 + Weak Topic Emergency Surgery',
    videoQuery: 'ICMA Pakistan OM M3 Full Course Revision Past Papers',
    technique:
      'Final 48h Protocol: Solve Full Mock 1 (12:45 PM) + Full Mock 2 (8:15 PM). Only drill concepts scoring under 75%.'
  },
  {
    topic: 'Day 9 (Dec 4 — Exam Eve): Final Mock 3 + Rapid Q.1–Q.10 Sweep + 11:30 PM Rest',
    videoQuery: 'ICMA M3 Organizational Management Quick Revision Summary',
    technique:
      'Exam-Eve Consolidation: No late-night panic cramming! Review Today’s 5 Flashcards, scan Past Paper Q.1–Q.10 headings once, sleep by 11:30 PM.'
  }
];

export function buildDailyBriefPlan(params: {
  weakTopics: string[];
  energyMode: DailyBriefPlan['energyMode'];
  completedSlotIds?: string[];
  now?: Date;
}): DailyBriefPlan {
  const { weakTopics, energyMode, completedSlotIds = [], now = new Date() } = params;
  const daysToExam = getDaysToDec5Exam(now);
  const dateKey = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(
    now.getDate()
  ).padStart(2, '0')}`;

  // Map days remaining from 9 -> Day 1 up to 0 -> Exam Day
  const dayIdx = Math.max(
    0,
    Math.min(EMERGENCY_NINE_DAY_ROADMAP.length - 1, 9 - Math.min(9, Math.max(1, daysToExam)))
  );
  const rotation = EMERGENCY_NINE_DAY_ROADMAP[dayIdx];
  const primaryWeak = weakTopics[0] || rotation.topic;
  const todayTopic = weakTopics.length
    ? `${rotation.topic} | Repair: ${weakTopics[0]}`
    : rotation.topic;

  let examPhaseTitle = 'EMERGENCY 9-DAY ZERO-TO-MERIT OM BATTLE PLAN';
  let upcomingAlert =
    'Weekly & Portion Grand Test coming in 2 days! Extra 25m revision & past-paper typing baked into tonight’s slot.';

  if (daysToExam === 0) {
    examPhaseTitle = '5 DECEMBER 2026 — FINAL OM EXAM DAY PROTOCOL';
    upcomingAlert =
      'EXAM TODAY (5 DEC)! Flip only your 5 confidence flashcards, breathe deeply, read MCQs twice, and leave 40% time for Typing questions.';
  } else if (daysToExam <= 2) {
    examPhaseTitle = 'FINAL 48-HOUR FULL-COURSE MOCK MARATHON (2–3 MOCKS)';
    upcomingAlert = `EMERGENCY ALERT: Only ${daysToExam} day(s) left before 5 Dec 2026! Attempt Full-Course Mock 1, 2 & 3 (30 MCQs + 5 Typing Qs) and repair every <70% topic immediately.`;
  } else if (daysToExam <= 5) {
    examPhaseTitle = 'HALF-BOOK & PORTION GRAND MOCK SPRINT (TO 5 DEC)';
    upcomingAlert = `Half-Book Mock & Portion Grand Tests due in 24–48 hrs (${daysToExam} days to 5 Dec). Today’s 5 flashcards prioritize your weakest units.`;
  }

  const morningMins = energyMode === 'low_recovery' ? 20 : 30;
  const officeBreak1Mins = energyMode === 'busy_office' ? 25 : 30;
  const officeBreak2Mins = energyMode === 'low_recovery' ? 10 : 15;
  const tiredCardMins = 20;
  const eveningDeepMins =
    energyMode === 'low_recovery' ? 35 : energyMode === 'busy_office' ? 45 : 60;
  const nightTestMins =
    energyMode === 'low_recovery' ? 40 : energyMode === 'final_sprint' ? 90 : 75;

  const rawSlots: Array<Omit<DailyRoutineSlot, 'completed'>> = [
    {
      id: 'slot_morning_8am',
      timeWindow: '08:00 AM – 08:30 AM',
      label: 'Morning Fresh Before Office (Zero-to-Hero Kickoff)',
      durationMinutes: morningMins,
      activity: `Read core unit notes + watch lecture clip on ${todayTopic}`,
      topicFocus: todayTopic,
      actionType: 'read_video'
    },
    {
      id: 'slot_office_break1',
      timeWindow: '12:45 PM – 01:15 PM',
      label: 'Office Lunch Break 1 + Short Test',
      durationMinutes: officeBreak1Mins,
      activity: `Revise 2 concepts + attempt "Short Test during Office" (10 MCQs focusing on ${primaryWeak})`,
      topicFocus: primaryWeak,
      actionType: 'office_short_test'
    },
    {
      id: 'slot_office_break2',
      timeWindow: '03:30 PM – 03:45 PM',
      label: 'Office Break 2 (Micro-Drill)',
      durationMinutes: officeBreak2Mins,
      activity: 'Scan 2 Past Paper 5-mark headings (Q.1–Q.10) & memorize key words',
      topicFocus: 'OM Past Papers Q1–Q10',
      actionType: 'read_video'
    },
    {
      id: 'slot_530_tired_cards',
      timeWindow: '05:30 PM – 05:50 PM',
      label: 'Post-Office Tired Recovery • 5 Flashcards',
      durationMinutes: tiredCardMins,
      activity:
        'Tired after office? Do ONLY Today’s 5 Curated Flashcards (yesterday + 2–3 days + upcoming topics) — zero heavy pressure',
      topicFocus: primaryWeak,
      actionType: 'flashcards'
    },
    {
      id: 'slot_evening_7pm',
      timeWindow: '07:00 PM – 08:00 PM',
      label: 'Fresh After Dinner/Rest — Deep OM Mastery',
      durationMinutes: eveningDeepMins,
      activity: `Deep textbook study + Past Papers + ACCA/CIMA/ICAEW angles on ${todayTopic}`,
      topicFocus: todayTopic,
      actionType: 'read_video'
    },
    {
      id: 'slot_8pm_recovery',
      timeWindow: '08:00 PM – 08:15 PM',
      label: '15-Min Nervous System Recovery',
      durationMinutes: 0,
      activity: 'Stand up, stretch shoulder/neck, drink water, 4-4-6 deep breathing before nightly test',
      topicFocus: 'Recovery & Focus Reset',
      actionType: 'recovery'
    },
    {
      id: 'slot_815_daily_test',
      timeWindow: '08:15 PM – 09:45 PM',
      label: 'Test of the Day (60% MCQ / 40% Typing)',
      durationMinutes: nightTestMins,
      activity:
        daysToExam <= 2
          ? 'Attempt Full-Course Mock (30 MCQs + 5 Typing Qs) & drill every mistake!'
          : 'Complete Test of the Day (22 MCQs + 4 Typing Qs); weak topics automatically added to tomorrow’s Short Test',
      topicFocus: primaryWeak,
      actionType: 'daily_test'
    },
    {
      id: 'slot_1130_sleep',
      timeWindow: '11:30 PM Sharp',
      label: 'Memory Consolidation Sleep',
      durationMinutes: 0,
      activity: 'Sleep strictly at 11:30 PM — brain converts short-term OM formulas into permanent exam recall!',
      topicFocus: 'Sleep Hygiene',
      actionType: 'recovery'
    }
  ];

  const slots: DailyRoutineSlot[] = rawSlots.map((s) => ({
    ...s,
    completed: completedSlotIds.includes(s.id)
  }));

  const totalStudyMinutes = slots.reduce((sum, s) => sum + s.durationMinutes, 0);
  const hoursStr = (totalStudyMinutes / 60).toFixed(1);

  const urduSpokenBrief = [
    `Assalam-o-Alaikum Beta! Ghabrana bilkul nahi — chahe aapki tayari abhi ZERO hai, 5 December 2026 ke OM paper mein ${daysToExam} din baqi hain aur hum aaj hi se 9-Day Emergency Merit Plan shuru kar rahe hain.`,
    `Aaj total ${hoursStr} ghantay (${totalStudyMinutes} minutes) smart targeted study hogi aapke office schedule ke mutabiq:`,
    `1. Subah 8:00 se 8:30 office se pehle: "${todayTopic}" parhein aur aaj ki recommended video dekh lein.`,
    `2. Dopahar 12:45 se 1:15 office break mein: "Short Test during Office" dein taake weak topics — khas tor par ${primaryWeak} — wahin pakkay hon.`,
    `3. Shaam 3:30 break mein 15 minute 2 Past Paper 5-mark sawalon ke headings dekhein.`,
    `4. Shaam 5:30 office se ghar aa kar aap thakay honge — heavy book nahi kholni! Sirf apni "Today's 5 Flashcards" flip karein.`,
    `5. Raat 7:00 se 8:00 fresh ho kar Past Papers aur ACCA/CIMA level practice karein, phir 15 minute complete break.`,
    `6. Raat 8:15 par "Test of the Day" (60% MCQs aur 40% Typing) dein, aur 11:30 baje so jayein.`,
    `Alert: ${upcomingAlert} Agar aaj office meeting ya thakan ho to mic daba kar Urdu mein mujhse plan adjust karwa lein!`
  ].join(' ');

  return {
    dateKey,
    daysToExam,
    examPhaseTitle,
    totalStudyMinutes,
    energyMode,
    todayTopic,
    todayVideoQuery: rotation.videoQuery,
    upcomingAlert,
    studyTechniqueTip: rotation.technique,
    urduSpokenBrief,
    slots
  };
}

export function selectTodayFiveFlashcards(
  cards: FlashcardItem[],
  weakTopics: string[]
): FlashcardItem[] {
  const weakLower = weakTopics.map((w) => w.toLowerCase());
  const scored = cards.map((card) => {
    let score = 0;
    const tag = (card.topicTag || '').toLowerCase();
    const text = `${card.front} ${card.back}`.toLowerCase();
    if (weakLower.some((w) => tag.includes(w.slice(0, 6)) || text.includes(w.slice(0, 6)))) {
      score += 100;
    }
    if (!card.mastered) score += 40;
    if (card.dayOffsetTag === -1) score += 25; // yesterday
    if (card.dayOffsetTag === -2) score += 20; // 2-3 days ago
    if (card.dayOffsetTag === 0) score += 18;  // today
    if (card.dayOffsetTag === 1) score += 15;  // upcoming
    score -= card.reviewCount * 3;
    return { card, score };
  });

  return scored
    .sort((a, b) => b.score - a.score)
    .slice(0, 5)
    .map((x) => x.card);
}

export function buildTestPaper(
  mode:
    | 'office_short'
    | 'daily_test'
    | 'weekly_grand'
    | 'portion_grand'
    | 'half_book_mock'
    | 'full_course_mock',
  weakTopics: string[],
  portionFilter?: string
): { title: string; mcqs: McqQuestionItem[]; typing: TypingQuestionItem[] } {
  const weakLower = weakTopics.map((w) => w.toLowerCase());
  const sortByWeak = (list: McqQuestionItem[]) =>
    [...list].sort((a, b) => {
      const aWeak = weakLower.some((w) => a.topicTag.toLowerCase().includes(w.slice(0, 5))) ? 1 : 0;
      const bWeak = weakLower.some((w) => b.topicTag.toLowerCase().includes(w.slice(0, 5))) ? 1 : 0;
      return bWeak - aWeak;
    });

  if (mode === 'office_short') {
    return {
      title: 'Short Test during Office (10 Weak-Topic MCQs)',
      mcqs: sortByWeak(OM_MCQ_QUESTIONS).slice(0, 10),
      typing: []
    };
  }
  if (mode === 'portion_grand') {
    const bank = portionFilter || 'part_a_ops';
    const subset = OM_MCQ_QUESTIONS.filter((q) => q.bankId === bank);
    const mcqs = subset.length ? subset : OM_MCQ_QUESTIONS.slice(0, 12);
    const typ = TYPING_QUESTIONS.filter((t) =>
      mcqs.some((m) => m.topicTag.toLowerCase().includes(t.topicTag.toLowerCase().slice(0, 5)))
    ).slice(0, 3);
    return {
      title: `Portion Grand Test (${bank.replace(/_/g, ' ').toUpperCase()})`,
      mcqs,
      typing: typ.length ? typ : TYPING_QUESTIONS.slice(0, 3)
    };
  }
  if (mode === 'half_book_mock') {
    const firstHalf = portionFilter !== 'second_half';
    const mcqs = OM_MCQ_QUESTIONS.filter((q) =>
      firstHalf
        ? q.bankId === 'part_a_ops' || q.bankId === 'part_b_hrm'
        : q.bankId === 'part_c_mkt' || q.bankId === 'part_d_epm'
    );
    return {
      title: firstHalf ? 'Half-Book Mock (Part A & B: Units 1–9)' : 'Half-Book Mock (Part C & D: Units 10–14)',
      mcqs,
      typing: TYPING_QUESTIONS.slice(firstHalf ? 0 : 5, firstHalf ? 4 : 9)
    };
  }
  if (mode === 'weekly_grand') {
    return {
      title: 'Weekly Grand Test (25 MCQs + 4 Typing Qs • 60/40)',
      mcqs: sortByWeak(OM_MCQ_QUESTIONS).slice(0, 25),
      typing: TYPING_QUESTIONS.slice(0, 4)
    };
  }
  if (mode === 'full_course_mock') {
    return {
      title: 'Final Full-Course ICMA Merit Mock (30 MCQs + 5 Typing Qs • 60/40)',
      mcqs: sortByWeak(OM_MCQ_QUESTIONS).slice(0, 30),
      typing: TYPING_QUESTIONS.slice(0, 5)
    };
  }
  // Default: daily_test
  return {
    title: 'Test of the Day (22 MCQs [60%] + 4 Typing Qs [40%])',
    mcqs: sortByWeak(OM_MCQ_QUESTIONS).slice(0, 22),
    typing: TYPING_QUESTIONS.slice(0, 4)
  };
}
