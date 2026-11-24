export type PortalTab =
  | 'home'
  | 'read'
  | 'tests'
  | 'flashcards'
  | 'tutor'
  | 'videos'
  | 'materials'
  | 'past';

export type TutorLang = 'en' | 'ur';

export type AiEngineMode = 'auto_free' | 'pollinations_direct';

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
  question: string;
  options: string[];
  answer: number;
  explanation: string;
}

export interface FlashcardItem {
  id: string;
  deck: string;
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
