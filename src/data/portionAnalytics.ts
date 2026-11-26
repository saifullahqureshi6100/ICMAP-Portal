import { ROADMAP_MILESTONES } from './icmapData';
import {
  FlashcardItem,
  OmPortionCode,
  OmPortionStat,
  TestAttemptSummary
} from '../types';

interface PortionBlueprint {
  code: OmPortionCode;
  partLabel: string;
  title: string;
  units: string;
  weightPercent: number;
  isHighYield: boolean;
  new2025Highlight: string;
  topics: string[];
  keywords: string[];
  milestoneIds: string[];
  routineSlotIds: string[];
  linkedBankFilter: 'part_a_ops' | 'part_b_hrm' | 'part_c_mkt' | 'part_d_epm';
  linkedChapterPage: number;
}

export const OM_SYLLABUS_BLUEPRINTS: PortionBlueprint[] = [
  {
    code: 'OM-A',
    partLabel: 'Part A',
    title: 'Operations Management',
    units: 'Units 1–7',
    weightPercent: 35,
    isHighYield: true,
    new2025Highlight: 'ESG Integration • AI-Driven Quality • 3D Printing',
    topics: [
      'Enterprise & PESTEL / ESG',
      'Mintzberg 5 Parts',
      'Job / Batch / Flow / 3D',
      '5S & TPM (OEE)',
      'MRP, ERP & OPT (DBR)',
      'Capacity & Queuing',
      'TQM, Six Sigma & Quality Circles'
    ],
    keywords: [
      'operations',
      'pestel',
      'esg',
      'mintzberg',
      'production',
      '3d',
      'tpm',
      'oee',
      '5s',
      'mrp',
      'opt',
      'erp',
      'quality',
      'juran',
      'six sigma',
      'kaizen',
      'lean',
      'part a',
      'units 1'
    ],
    milestoneIds: ['ms_1', 'ms_2', 'ms_5'],
    routineSlotIds: ['slot_morning_8am', 'slot_office_break1', 'slot_office_break2', 'slot_815_daily_test'],
    linkedBankFilter: 'part_a_ops',
    linkedChapterPage: 8
  },
  {
    code: 'OM-B',
    partLabel: 'Part B',
    title: 'Human Resource Management',
    units: 'Units 8–9',
    weightPercent: 20,
    isHighYield: false,
    new2025Highlight: 'Algorithmic Recruitment Bias • Remote/Hybrid • EX',
    topics: [
      'Human Capital & Motivation',
      'Handy Shamrock & Hybrid Teams',
      'Full HR Cycle & Agencies',
      'Interview Judgment Biases',
      'Pfeffer 7 Practices & Kolb Cycle'
    ],
    keywords: [
      'hrm',
      'human resource',
      'human capital',
      'motivation',
      'herzberg',
      'maslow',
      'shamrock',
      'handy',
      'pfeffer',
      'kolb',
      'interview',
      'halo',
      'bias',
      'outsourc',
      'agency',
      '70-20-10',
      'part b',
      'units 8'
    ],
    milestoneIds: ['ms_3', 'ms_5'],
    routineSlotIds: ['slot_530_tired_cards', 'slot_evening_7pm', 'slot_815_daily_test'],
    linkedBankFilter: 'part_b_hrm',
    linkedChapterPage: 87
  },
  {
    code: 'OM-C',
    partLabel: 'Part C',
    title: 'Marketing Management',
    units: 'Units 10–12',
    weightPercent: 25,
    isHighYield: true,
    new2025Highlight: 'Metaverse / Virtual Branding • Web3/DAO • CSR',
    topics: [
      'Marketing vs Sales Orientation',
      'STP & Virtual Branding',
      '4Ps / 7Ps Services Mix',
      'BCG & Ansoff Growth Matrix',
      'Consumer Behavior & CSR'
    ],
    keywords: [
      'marketing',
      'sales orientation',
      'stp',
      '7ps',
      '4ps',
      'bcg',
      'ansoff',
      'consumer',
      'brand',
      'metaverse',
      'web3',
      'csr',
      'part c',
      'units 10'
    ],
    milestoneIds: ['ms_4', 'ms_5'],
    routineSlotIds: ['slot_morning_8am', 'slot_530_tired_cards', 'slot_815_daily_test'],
    linkedBankFilter: 'part_c_mkt',
    linkedChapterPage: 120
  },
  {
    code: 'OM-D',
    partLabel: 'Part D',
    title: 'Performance Management',
    units: 'Units 13–14',
    weightPercent: 20,
    isHighYield: false,
    new2025Highlight: 'EPM Control • Budgetary Execution • Balanced Scorecard',
    topics: [
      'Enterprise Performance Mgmt',
      'CSFs vs KPIs',
      'Budgetary Control Execution',
      'Balanced Scorecard (4 Lenses)',
      'Building Block & Performance Pyramid'
    ],
    keywords: [
      'performance',
      'epm',
      'budget',
      'csf',
      'kpi',
      'balanced scorecard',
      'bsc',
      'fitzgerald',
      'building block',
      'pyramid',
      'lynch',
      'variance',
      'part d',
      'units 13'
    ],
    milestoneIds: ['ms_4', 'ms_5'],
    routineSlotIds: ['slot_office_break1', 'slot_evening_7pm', 'slot_815_daily_test'],
    linkedBankFilter: 'part_d_epm',
    linkedChapterPage: 215
  }
];

export function mapTopicToPortionCode(topicOrBankId: string): OmPortionCode {
  const lower = topicOrBankId.toLowerCase();
  if (lower.includes('part_b') || lower.includes('om-b')) return 'OM-B';
  if (lower.includes('part_c') || lower.includes('om-c')) return 'OM-C';
  if (lower.includes('part_d') || lower.includes('om-d')) return 'OM-D';
  if (lower.includes('part_a') || lower.includes('om-a')) return 'OM-A';

  for (const bp of OM_SYLLABUS_BLUEPRINTS) {
    if (bp.keywords.some((kw) => lower.includes(kw))) {
      return bp.code;
    }
  }
  return 'OM-A';
}

export function computeOmPortionStats(params: {
  flashcards: FlashcardItem[];
  completedMilestones: string[];
  completedSlotIds: string[];
  readProgressMap: Record<string, number>;
  testHistory: TestAttemptSummary[];
}): OmPortionStat[] {
  const { flashcards, completedMilestones, completedSlotIds, readProgressMap, testHistory } = params;
  const bookCompleted = (readProgressMap['om_study_text_2026'] || 0) > 0;
  const pastPaperCompleted = (readProgressMap['om_past_papers_ml1'] || 0) > 0;

  return OM_SYLLABUS_BLUEPRINTS.map((bp) => {
    // 1. Concepts Mastery (Flashcards in portion + reading status)
    const portionCards = flashcards.filter((c) => {
      const hay = `${c.topicTag || ''} ${c.front} ${c.deck}`.toLowerCase();
      return bp.keywords.some((kw) => hay.includes(kw));
    });
    const masteredOrReviewed = portionCards.reduce((sum, card) => {
      if (card.mastered) return sum + 1;
      if (card.reviewCount > 0) return sum + 0.55;
      return sum;
    }, 0);
    const cardRatio = portionCards.length > 0 ? masteredOrReviewed / portionCards.length : 0;
    const readingBonus = (bookCompleted ? 0.25 : 0) + (pastPaperCompleted ? 0.15 : 0);
    const conceptsPct = Math.min(100, Math.round(cardRatio * 60 + readingBonus * 100));

    // 2. Plan & Routine Execution
    const validMilestones = bp.milestoneIds.filter((id) =>
      ROADMAP_MILESTONES.some((m) => m.id === id)
    );
    const doneMilestones = validMilestones.filter((id) =>
      completedMilestones.includes(id)
    ).length;
    const msRatio = validMilestones.length > 0 ? doneMilestones / validMilestones.length : 0;
    const doneSlots = bp.routineSlotIds.filter((id) => completedSlotIds.includes(id)).length;
    const slotRatio = bp.routineSlotIds.length > 0 ? doneSlots / bp.routineSlotIds.length : 0;
    const planPct = Math.min(100, Math.round(msRatio * 70 + slotRatio * 30));

    // 3. Test Marks Performance
    let rightSum = 0;
    let totalSum = 0;
    let matchedAttempts = 0;

    testHistory.forEach((attempt) => {
      const pScore = attempt.portionScores?.[bp.code];
      if (pScore && pScore.total > 0) {
        rightSum += pScore.right;
        totalSum += pScore.total;
        matchedAttempts += 1;
      } else {
        const titleLower = attempt.title.toLowerCase();
        const isPortionSpecific = bp.keywords.some((kw) => titleLower.includes(kw));
        const isComprehensive =
          titleLower.includes('daily') ||
          titleLower.includes('full-course') ||
          titleLower.includes('short test') ||
          titleLower.includes('weekly');
        if ((isPortionSpecific || isComprehensive) && attempt.mcqTotal > 0) {
          rightSum += attempt.mcqCorrect;
          totalSum += attempt.mcqTotal;
          matchedAttempts += 1;
        }
      }
    });

    const testMarksPct =
      totalSum > 0 ? Math.min(100, Math.round((rightSum / totalSum) * 100)) : null;

    // 4. Overall Preparation Till Now & Projected Exam Contribution
    const overallPrepPct =
      testMarksPct !== null
        ? Math.round(testMarksPct * 0.45 + conceptsPct * 0.35 + planPct * 0.2)
        : Math.round(conceptsPct * 0.6 + planPct * 0.4);

    const projectedMarks = Number(((overallPrepPct / 100) * bp.weightPercent).toFixed(1));

    return {
      code: bp.code,
      partLabel: bp.partLabel,
      title: bp.title,
      units: bp.units,
      weightPercent: bp.weightPercent,
      isHighYield: bp.isHighYield,
      new2025Highlight: bp.new2025Highlight,
      topics: bp.topics,
      linkedBankFilter: bp.linkedBankFilter,
      linkedChapterPage: bp.linkedChapterPage,
      conceptsPct,
      planPct,
      testMarksPct,
      testAttemptsCount: matchedAttempts,
      overallPrepPct,
      projectedMarks
    };
  });
}
