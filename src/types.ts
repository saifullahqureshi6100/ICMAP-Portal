export type PortalTab =
  | 'home'
  | 'read'
  | 'flashcards'
  | 'tests'
  | 'tutor'
  | 'videos'
  | 'materials'
  | 'past';

export type TutorLang = 'en' | 'ur';

export type AiEngineMode = 'auto_free' | 'pollinations_direct';

export type TestModeKind =
  | 'office_short'
  | 'daily_test'
  | 'weekly_grand'
  | 'portion_grand'
  | 'half_book_mock'
  | 'full_course_mock';

export interface SubjectInfo {
  code: string;
  name: string;
  progress: number;
  weak: string[];
}

export interface PdfChapterJump {
  title: string;
  pageNumber: number;
}

export interface StudyMaterialItem {
  id: string;
  title: string;
  category: string;
  type: 'pdf' | 'notes' | 'drive' | 'video';
  description: string;
  pdfUrl?: string;
  externalUrl?: string;
  estimatedMinutes: number;
  chapters?: PdfChapterJump[];
  readingContent: string;
}

export interface McqQuestionItem {
  id: string;
  bankId: string;
  topicTag: string;
  sourceBenchmark?: string;
  question: string;
  options: string[];
  answer: number;
  explanation: string;
}

export interface TypingQuestionItem {
  id: string;
  topicTag: string;
  marks: number;
  sourceBenchmark: string;
  question: string;
  keywords: string[];
  modelAnswer: string;
}

export interface FlashcardItem {
  id: string;
  deck: string;
  topicTag?: string;
  dayOffsetTag?: number; // 0 = today, -1 = yesterday, -2 = 2 days ago, +1 = upcoming
  front: string;
  back: string;
  hint?: string;
  mastered: boolean;
  reviewCount: number;
}

export interface RoadmapMilestone {
  id: string;
  unitLabel: string;
  title: string;
  description: string;
  targetMinutes: number;
  linkedMaterialId: string;
  linkedTopic: string;
}

export interface DailyRoutineSlot {
  id: string;
  timeWindow: string;
  label: string;
  durationMinutes: number;
  activity: string;
  topicFocus: string;
  actionType: 'read_video' | 'office_short_test' | 'flashcards' | 'daily_test' | 'recovery';
  completed: boolean;
}

export interface DailyBriefPlan {
  dateKey: string;
  daysToExam: number;
  examPhaseTitle: string;
  totalStudyMinutes: number;
  energyMode: 'normal' | 'busy_office' | 'low_recovery' | 'final_sprint';
  todayTopic: string;
  todayVideoQuery: string;
  upcomingAlert: string;
  studyTechniqueTip: string;
  urduSpokenBrief: string;
  slots: DailyRoutineSlot[];
}

export interface TestAttemptSummary {
  id: string;
  kind: TestModeKind;
  title: string;
  mcqCorrect: number;
  mcqTotal: number;
  typingCompleted: number;
  typingTotal: number;
  weakTopicsDetected: string[];
  timestamp: number;
}

export type PomodoroPhase = 'work' | 'short_break' | 'long_break';

export interface PomodoroSettings {
  workMinutes: number;
  shortBreakMinutes: number;
  longBreakMinutes: number;
  cyclesBeforeLongBreak: number;
  autoAdvance: boolean;
}

export interface TutorChatItem {
  id: string;
  role: 'user' | 'tutor';
  text: string;
  provider?: string;
}

export interface FreeReferenceItem {
  sourceName: string;
  title: string;
  subtitle: string;
  summary: string;
  url: string;
}

export interface UploadedLocalMaterial {
  name: string;
  size: number;
  type: string;
  date: number;
}
