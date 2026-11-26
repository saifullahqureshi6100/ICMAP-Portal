import React, { useEffect, useMemo, useState } from 'react';
import {
  BarChart3,
  BookOpen,
  Brain,
  CheckCircle2,
  Download,
  ExternalLink,
  FileText,
  Globe,
  GraduationCap,
  Layers,
  Maximize2,
  Menu,
  Mic,
  Minimize2,
  Moon,
  Play,
  Search,
  Send,
  Sun,
  Upload,
  Video,
  Volume2,
  VolumeX,
  X
} from 'lucide-react';
import { DailyBriefAndCoach } from './components/DailyBriefAndCoach';
import { DailyTestsArena } from './components/DailyTestsArena';
import { ExecutiveDashboard } from './components/ExecutiveDashboard';
import { FlashcardDeck } from './components/FlashcardDeck';
import { PdfAndReaderView } from './components/PdfAndReaderView';
import { PomodoroTimer } from './components/PomodoroTimer';
import { StudyRoadmap } from './components/StudyRoadmap';
import {
  OM_MCQ_QUESTIONS,
  ROADMAP_MILESTONES,
  STARTER_FLASHCARDS,
  STUDY_MATERIALS,
  SUBJECTS
} from './data/icmapData';
import { computeOmPortionStats } from './data/portionAnalytics';
import {
  buildDailyBriefPlan,
  selectTodayFiveFlashcards
} from './data/strategistEngine';
import {
  callFreeAiTutor,
  fetchDictionaryWord,
  fetchWikipediaConcept,
  parseMcqArrayFromAi,
  parseTypingQuestionsFromAi
} from './lib/freeAiClient';
import {
  preloadMaleVoices,
  speakWithMaleTutor,
  stopTutorSpeech
} from './lib/maleVoiceTts';
import {
  AiEngineMode,
  DailyBriefPlan,
  DailyRoutineSlot,
  FlashcardItem,
  FreeReferenceItem,
  McqQuestionItem,
  PomodoroSettings,
  PortalTab,
  RoadmapMilestone,
  StudyMaterialItem,
  SubjectInfo,
  TestAttemptSummary,
  TestModeKind,
  TutorChatItem,
  TutorLang,
  TypingQuestionItem,
  UploadedLocalMaterial
} from './types';

interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>;
}

function loadStored<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key);
    return raw ? (JSON.parse(raw) as T) : fallback;
  } catch {
    return fallback;
  }
}

function saveStored<T>(key: string, value: T) {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch {
    // Ignore storage quota errors
  }
}

function getLocalDateKey(date = new Date()): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

export default function App() {
  const [tab, setTab] = useState<PortalTab>('home');
  const [subject, setSubject] = useState<SubjectInfo>(SUBJECTS[0]);
  const [topic, setTopic] = useState<string>(SUBJECTS[0].weak[0]);
  const [selectedMaterial, setSelectedMaterial] = useState<StudyMaterialItem>(STUDY_MATERIALS[0]);
  const [lang, setLang] = useState<TutorLang>('ur');
  const [voiceEnabled, setVoiceEnabled] = useState(true);
  const [isSpeaking, setIsSpeaking] = useState(false);
  const [engineMode, setEngineMode] = useState<AiEngineMode>('auto_free');
  const [themeMode, setThemeMode] = useState<'midnight' | 'light'>(() =>
    loadStored<'midnight' | 'light'>('icmap_theme', 'midnight')
  );

  useEffect(() => {
    document.documentElement.dataset.theme = themeMode;
    saveStored('icmap_theme', themeMode);
    const meta = document.querySelector('meta[name="theme-color"]');
    if (meta) {
      meta.setAttribute('content', themeMode === 'light' ? '#f8fafc' : '#07111f');
    }
  }, [themeMode]);

  function toggleGlobalTheme() {
    setThemeMode((prev) => (prev === 'midnight' ? 'light' : 'midnight'));
  }

  // Master Strategist Adaptive State
  const [weakTopics, setWeakTopics] = useState<string[]>(() =>
    loadStored('icmap_weak_topics', SUBJECTS[0].weak)
  );
  const [energyMode, setEnergyMode] = useState<DailyBriefPlan['energyMode']>(() =>
    loadStored('icmap_energy_mode', 'normal')
  );
  const [completedSlotIds, setCompletedSlotIds] = useState<string[]>(() =>
    loadStored('icmap_daily_slots_done', [])
  );
  const [testHistory, setTestHistory] = useState<TestAttemptSummary[]>(() =>
    loadStored('icmap_test_history', [])
  );
  const [preferredTestMode, setPreferredTestMode] = useState<TestModeKind>('daily_test');
  const [preferredPortionFilter, setPreferredPortionFilter] = useState<string>('part_a_ops');
  const [defaultTodayFiveCards, setDefaultTodayFiveCards] = useState(true);

  const dailyPlan = useMemo(
    () =>
      buildDailyBriefPlan({
        weakTopics,
        energyMode,
        completedSlotIds
      }),
    [weakTopics, energyMode, completedSlotIds]
  );

  // Full-screen & PWA install state
  const [isFullScreen, setIsFullScreen] = useState(false);
  const [fullScreenTarget, setFullScreenTarget] = useState<'active' | 'pomodoro' | 'roadmap'>('active');
  const [installPromptEvent, setInstallPromptEvent] = useState<BeforeInstallPromptEvent | null>(null);
  const [isStandalonePwa, setIsStandalonePwa] = useState(false);
  const [showInstallHelp, setShowInstallHelp] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  // Study Progress, Pomodoro & Roadmap Persistence
  const [studyMinutesTotal, setStudyMinutesTotal] = useState<number>(() =>
    loadStored('icmap_study_minutes', 0)
  );
  const [dailyStudyMinutes, setDailyStudyMinutes] = useState<Record<string, number>>(() => {
    const stored = loadStored<Record<string, number>>('icmap_daily_study_minutes', {});
    const total = loadStored<number>('icmap_study_minutes', 0);
    if (Object.keys(stored).length === 0 && total > 0) {
      return { [getLocalDateKey()]: total };
    }
    return stored;
  });

  function logStudyMinutes(minutes: number, sourceLabel?: string) {
    const safeMinutes = Math.max(1, Math.round(minutes));
    const todayKey = getLocalDateKey();
    setStudyMinutesTotal((prev) => {
      const updated = prev + safeMinutes;
      saveStored('icmap_study_minutes', updated);
      return updated;
    });
    setDailyStudyMinutes((prev) => {
      const updated = {
        ...prev,
        [todayKey]: (prev[todayKey] || 0) + safeMinutes
      };
      saveStored('icmap_daily_study_minutes', updated);
      return updated;
    });
    showToast(
      sourceLabel
        ? `Logged +${safeMinutes}m (${sourceLabel})!`
        : `Focus session completed! +${safeMinutes} minutes logged.`
    );
  }
  const [pomodoroSettings, setPomodoroSettings] = useState<PomodoroSettings>(() =>
    loadStored('icmap_pomodoro_cfg', {
      workMinutes: 25,
      shortBreakMinutes: 5,
      longBreakMinutes: 15,
      cyclesBeforeLongBreak: 4,
      autoAdvance: true
    })
  );
  const [completedMilestones, setCompletedMilestones] = useState<string[]>(() =>
    loadStored('icmap_milestones', [])
  );
  const [readProgressMap, setReadProgressMap] = useState<Record<string, number>>(() =>
    loadStored('icmap_read_progress', {})
  );
  const [savedNotesMap, setSavedNotesMap] = useState<Record<string, string>>(() =>
    loadStored('icmap_material_notes', {})
  );

  // Tutor, Free Wikipedia / Dictionary Reference
  const [question, setQuestion] = useState('');
  const [chat, setChat] = useState<TutorChatItem[]>(() => [
    {
      id: 'welcome',
      role: 'tutor',
      text: `Assalam-o-Alaikum Beta! Main aapka 30-Year Finance Strategist aur 50-Year Master Teacher hoon. 5 December 2026 OM exam mein ${dailyPlan.daysToExam} din baqi hain — zero preparation se Pakistan Certificate of Merit tak! Upar "Brief of the Day (Urdu)" dabaein ya apni office meeting, thakan, ya kisi bhi OM topic par Urdu mein sawal karein!`,
      provider: 'Groq + Free AI Master Strategist'
    }
  ]);
  const [busy, setBusy] = useState(false);
  const [listeningVoice, setListeningVoice] = useState(false);
  const [tutorError, setTutorError] = useState('');
  const [refQuery, setRefQuery] = useState('');
  const [refResult, setRefResult] = useState<FreeReferenceItem | null>(null);
  const [refBusy, setRefBusy] = useState(false);
  const [refError, setRefError] = useState('');

  // Flashcards
  const [flashcards, setFlashcards] = useState<FlashcardItem[]>(() =>
    loadStored('icmap_flashcards', STARTER_FLASHCARDS)
  );
  const [generatingCards, setGeneratingCards] = useState(false);

  const todayFiveCardIds = useMemo(
    () => selectTodayFiveFlashcards(flashcards, weakTopics).map((c) => c.id),
    [flashcards, weakTopics]
  );

  // Videos & Materials
  const [videos, setVideos] = useState<
    Array<{ id: string; title: string; channel: string; thumbnail: string }>
  >([]);
  const [videoQuery, setVideoQuery] = useState(dailyPlan.todayVideoQuery);
  const [uploadedMaterials, setUploadedMaterials] = useState<UploadedLocalMaterial[]>(() =>
    loadStored('icmap_uploaded_files', [])
  );
  const [driveLink, setDriveLink] = useState<string>(() =>
    loadStored('icmap_drive_link', 'https://drive.google.com/drive/folders/1JRAADz_hSw7MLr4_Pl6fuhuyRhlyt3ex')
  );
  const [toastMsg, setToastMsg] = useState('');

  useEffect(() => {
    setTopic(weakTopics[0] || subject.weak[0] || 'Core Concepts');
  }, [subject, weakTopics]);

  useEffect(() => {
    const mq = window.matchMedia('(display-mode: standalone)');
    setIsStandalonePwa(mq.matches);

    const onBeforeInstall = (e: Event) => {
      e.preventDefault();
      setInstallPromptEvent(e as BeforeInstallPromptEvent);
    };
    const onFullscreenChange = () => {
      if (!document.fullscreenElement) {
        setIsFullScreen(false);
      }
    };
    preloadMaleVoices();
    window.addEventListener('beforeinstallprompt', onBeforeInstall);
    document.addEventListener('fullscreenchange', onFullscreenChange);
    return () => {
      window.removeEventListener('beforeinstallprompt', onBeforeInstall);
      document.removeEventListener('fullscreenchange', onFullscreenChange);
    };
  }, []);

  function speakText(text: string, targetLang: TutorLang) {
    speakWithMaleTutor(text, targetLang, {
      onStart: () => setIsSpeaking(true),
      onEnd: () => setIsSpeaking(false),
      onError: () => setIsSpeaking(false)
    });
  }

  function stopSpeech() {
    stopTutorSpeech();
    setIsSpeaking(false);
  }

  const portionStats = useMemo(
    () =>
      computeOmPortionStats({
        flashcards,
        completedMilestones,
        completedSlotIds,
        readProgressMap,
        testHistory
      }),
    [flashcards, completedMilestones, completedSlotIds, readProgressMap, testHistory]
  );

  const overall = useMemo(
    () => Math.round(portionStats.reduce((acc, p) => acc + p.projectedMarks, 0)),
    [portionStats]
  );

  function showToast(msg: string) {
    setToastMsg(msg);
    window.setTimeout(() => setToastMsg((cur) => (cur === msg ? '' : cur)), 3600);
  }

  async function toggleFullScreen(target: 'active' | 'pomodoro' | 'roadmap' = 'active') {
    if (!isFullScreen) {
      setFullScreenTarget(target);
      setIsFullScreen(true);
      try {
        if (document.documentElement.requestFullscreen) {
          await document.documentElement.requestFullscreen();
        }
      } catch {
        // Immersive React layout works even if browser blocks native Fullscreen API
      }
    } else {
      setIsFullScreen(false);
      try {
        if (document.fullscreenElement && document.exitFullscreen) {
          await document.exitFullscreen();
        }
      } catch {
        // Ignore
      }
    }
  }

  async function handleInstallPwa() {
    if (installPromptEvent) {
      await installPromptEvent.prompt();
      const choice = await installPromptEvent.userChoice;
      if (choice.outcome === 'accepted') {
        setInstallPromptEvent(null);
        showToast('ICMAP Smart Tutor installed!');
      }
    } else {
      setShowInstallHelp(true);
    }
  }

  function startVoiceQuestion() {
    const SpeechRec =
      (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SpeechRec) {
      showToast('Voice recognition works best in Chrome or Edge.');
      return;
    }
    const recognition = new SpeechRec();
    recognition.lang = lang === 'ur' ? 'ur-PK' : 'en-US';
    recognition.interimResults = false;
    recognition.maxAlternatives = 1;
    setListeningVoice(true);
    recognition.onresult = (event: any) => {
      const spoken = String(event?.results?.[0]?.[0]?.transcript || '').trim();
      setListeningVoice(false);
      if (spoken) {
        void askTutor(spoken);
      }
    };
    recognition.onerror = () => {
      setListeningVoice(false);
      showToast('Could not capture voice. Check microphone permission.');
    };
    recognition.onend = () => {
      setListeningVoice(false);
    };
    recognition.start();
  }

  async function askTutor(customPrompt?: string) {
    const q = (customPrompt ?? question).trim();
    if (!q || busy) return;
    setTutorError('');
    setChat((prev) => [...prev, { id: `u_${Date.now()}`, role: 'user', text: q }]);
    if (!customPrompt) setQuestion('');
    setBusy(true);
    try {
      // Detect fatigue / meeting intent to adapt today's routine dynamically
      const lower = q.toLowerCase();
      if (lower.includes('meeting') || lower.includes('busy')) {
        setEnergyMode('busy_office');
        saveStored('icmap_energy_mode', 'busy_office');
      } else if (lower.includes('tired') || lower.includes('thak') || lower.includes('tension') || lower.includes('stress')) {
        setEnergyMode('low_recovery');
        saveStored('icmap_energy_mode', 'low_recovery');
      }

      const res = await callFreeAiTutor({
        action: 'chat',
        subject: subject.code,
        topic: `${topic} | Days to 5 Dec 2026 Exam: ${dailyPlan.daysToExam} | Weak Topics: ${weakTopics.join(', ')}`,
        question: q,
        language: lang,
        context: `${selectedMaterial.title}\nCurrent Weak Areas: ${weakTopics.join(', ')}`,
        engineMode
      });
      setChat((prev) => [
        ...prev,
        { id: `t_${Date.now()}`, role: 'tutor', text: res.text, provider: res.provider }
      ]);
      if (voiceEnabled) speakText(res.text, lang);
    } catch (e: any) {
      setTutorError(e?.message || 'Could not reach AI endpoint.');
    } finally {
      setBusy(false);
    }
  }

  async function generateAiDailyBrief(): Promise<string> {
    const res = await callFreeAiTutor({
      action: 'daily_brief',
      subject: 'ICMA M3 Organizational Management (ML-1)',
      topic: dailyPlan.todayTopic,
      language: 'ur',
      context: `Days left before 5 Dec 2026: ${dailyPlan.daysToExam}. Starting from zero preparation. Weak Topics: ${weakTopics.join(
        ', '
      )}. Energy mode: ${energyMode}. Total planned minutes today: ${dailyPlan.totalStudyMinutes}.`,
      engineMode
    });
    return res.text;
  }

  async function runSelectionAi(
    action: 'summarize_bullets' | 'explain_concept',
    selectedText: string,
    material: StudyMaterialItem
  ): Promise<string> {
    const res = await callFreeAiTutor({
      action,
      subject: subject.code,
      topic: material.title,
      language: lang,
      context: selectedText,
      engineMode
    });
    return res.text;
  }

  async function askTeacherInsideReader(
    userQ: string,
    material: StudyMaterialItem,
    inUrdu: boolean
  ): Promise<string> {
    const res = await callFreeAiTutor({
      action: 'chat',
      subject: 'ICMA M3 OM',
      topic: material.title,
      question: userQ,
      language: inUrdu ? 'ur' : 'en',
      context: `Material: ${material.title}\nExcerpt:\n${material.readingContent.slice(0, 2200)}`,
      engineMode
    });
    return res.text;
  }

  async function lookupWebConceptSummary(query: string): Promise<string> {
    const res = await fetchWikipediaConcept(query);
    return `${res.title} (${res.subtitle}): ${res.summary}`;
  }

  async function generateAiTest(
    action: 'mcq' | 'numerical' | 'typing',
    targetTopic: string,
    count: number
  ): Promise<{ mcqs: McqQuestionItem[]; typing: TypingQuestionItem[] }> {
    setBusy(true);
    try {
      const res = await callFreeAiTutor({
        action,
        subject: 'ICMA M3 OM / ACCA / CIMA / ICAEW',
        topic: targetTopic,
        count,
        language: 'en',
        context: selectedMaterial.readingContent,
        engineMode
      });
      if (action === 'typing') {
        const typing = parseTypingQuestionsFromAi(res.parsed, targetTopic);
        return { mcqs: [], typing };
      }
      const mcqs = parseMcqArrayFromAi(res.parsed, `ai_${action}`, targetTopic);
      return { mcqs, typing: [] };
    } finally {
      setBusy(false);
    }
  }

  async function gradeTypedAnswer(
    questionItem: TypingQuestionItem,
    userAnswer: string
  ): Promise<string> {
    const res = await callFreeAiTutor({
      action: 'grade_typing',
      subject: 'ICMA M3 OM (ML-1)',
      topic: questionItem.topicTag,
      language: lang,
      context: `Exam Question (${questionItem.marks} Marks): ${questionItem.question}\nModel Answer Keywords: ${questionItem.keywords.join(
        ', '
      )}\nOfficial Suggested Answer:\n${questionItem.modelAnswer}\n\nStudent Typed Answer:\n${userAnswer}`,
      engineMode
    });
    return res.text;
  }

  function handleTestCompleted(summary: TestAttemptSummary) {
    const nextHistory = [summary, ...testHistory].slice(0, 30);
    setTestHistory(nextHistory);
    saveStored('icmap_test_history', nextHistory);

    const earnedMinutes = Math.max(
      5,
      Math.round(summary.mcqTotal * 1 + summary.typingCompleted * 5)
    );
    logStudyMinutes(earnedMinutes, summary.title);

    if (summary.weakTopicsDetected.length > 0) {
      const mergedWeak = Array.from(
        new Set([...summary.weakTopicsDetected, ...weakTopics])
      ).slice(0, 8);
      setWeakTopics(mergedWeak);
      saveStored('icmap_weak_topics', mergedWeak);
      showToast(
        `+${earnedMinutes}m logged • Plan updated for: ${summary.weakTopicsDetected.join(', ')}`
      );
    } else {
      showToast(
        `Strong Merit Score (+${earnedMinutes}m logged)! Moving forward on your 5 Dec 2026 roadmap.`
      );
    }
  }

  async function generateFlashcardsContent(sourceContext?: string, count = 5) {
    setBusy(true);
    setGeneratingCards(true);
    try {
      const res = await callFreeAiTutor({
        action: 'flashcards',
        subject: subject.code,
        topic: `${topic} (Weak areas: ${weakTopics.slice(0, 3).join(', ')})`,
        count,
        language: 'en',
        context: sourceContext || selectedMaterial.readingContent,
        engineMode
      });
      const rawCards = Array.isArray(res.parsed?.cards) ? res.parsed.cards : [];
      const created: FlashcardItem[] = rawCards
        .filter((c: any) => c?.front && c?.back)
        .map((c: any, i: number) => ({
          id: `ai_fc_${Date.now()}_${i}`,
          deck: selectedMaterial.title,
          topicTag: topic,
          dayOffsetTag: 0,
          front: String(c.front).trim(),
          back: String(c.back).trim(),
          hint: c.hint ? String(c.hint).trim() : '',
          mastered: false,
          reviewCount: 0
        }));
      if (created.length > 0) {
        const updated = [...created, ...flashcards];
        setFlashcards(updated);
        saveStored('icmap_flashcards', updated);
        setDefaultTodayFiveCards(true);
        setTab('flashcards');
        showToast(`Created ${created.length} daily flashcards!`);
      } else {
        throw new Error('Could not parse AI flashcard JSON. Please try again.');
      }
    } catch (e: any) {
      showToast(e?.message || 'Could not generate flashcards.');
    } finally {
      setBusy(false);
      setGeneratingCards(false);
    }
  }

  async function runFreeLookup(kind: 'wiki' | 'dict') {
    if (!refQuery.trim()) return;
    setRefBusy(true);
    setRefError('');
    try {
      const result =
        kind === 'wiki'
          ? await fetchWikipediaConcept(refQuery)
          : await fetchDictionaryWord(refQuery);
      setRefResult(result);
    } catch (e: any) {
      setRefError(e?.message || 'Lookup failed.');
    } finally {
      setRefBusy(false);
    }
  }

  async function findVideos(overrideQuery?: string) {
    const q = (overrideQuery ?? videoQuery).trim();
    if (!q) return;
    setVideoQuery(q);
    setBusy(true);
    try {
      const r = await fetch(`/api/youtube?q=${encodeURIComponent(q)}`);
      if (r.ok) {
        const d = await r.json();
        setVideos(d.items || []);
        if (!(d.items || []).length && d.searchUrl) {
          window.open(d.searchUrl, '_blank');
        }
        return;
      }
    } catch {
      // Fallback launches direct YouTube topic search
    } finally {
      setBusy(false);
    }
    window.open(
      `https://www.youtube.com/results?search_query=${encodeURIComponent(q)}`,
      '_blank'
    );
  }

  function handleLaunchRoutineAction(
    actionType: DailyRoutineSlot['actionType'],
    topicFocus: string
  ) {
    setTopic(topicFocus);
    if (actionType === 'read_video') {
      setSelectedMaterial(
        topicFocus.toLowerCase().includes('past') ? STUDY_MATERIALS[1] : STUDY_MATERIALS[0]
      );
      setTab('read');
    } else if (actionType === 'office_short_test') {
      setPreferredTestMode('office_short');
      setTab('tests');
    } else if (actionType === 'daily_test') {
      setPreferredTestMode('daily_test');
      setTab('tests');
    } else if (actionType === 'flashcards') {
      setDefaultTodayFiveCards(true);
      setTab('flashcards');
    }
  }

  function handleMarkRead(materialId: string) {
    const next = {
      ...readProgressMap,
      [materialId]: Math.min(100, (readProgressMap[materialId] || 0) + 25)
    };
    setReadProgressMap(next);
    saveStored('icmap_read_progress', next);
    showToast('Reading progress saved!');
  }

  function handleSaveNotes(materialId: string, notes: string) {
    const next = { ...savedNotesMap, [materialId]: notes };
    setSavedNotesMap(next);
    saveStored('icmap_material_notes', next);
    showToast('Study notes saved locally.');
  }

  function handleToggleMilestone(id: string) {
    const next = completedMilestones.includes(id)
      ? completedMilestones.filter((m) => m !== id)
      : [...completedMilestones, id];
    setCompletedMilestones(next);
    saveStored('icmap_milestones', next);
  }

  function handleStartMilestone(ms: RoadmapMilestone) {
    const mat = STUDY_MATERIALS.find((m) => m.id === ms.linkedMaterialId) || STUDY_MATERIALS[0];
    setSelectedMaterial(mat);
    setTopic(ms.linkedTopic);
    setTab('read');
  }

  function handleFlashcardReview(cardId: string, mastered: boolean) {
    const next = flashcards.map((c) =>
      c.id === cardId ? { ...c, mastered, reviewCount: c.reviewCount + 1 } : c
    );
    setFlashcards(next);
    saveStored('icmap_flashcards', next);
  }

  function handleAddCustomCard(deck: string, front: string, back: string, hint: string) {
    const card: FlashcardItem = {
      id: `custom_${Date.now()}`,
      deck,
      topicTag: topic,
      dayOffsetTag: 0,
      front,
      back,
      hint,
      mastered: false,
      reviewCount: 0
    };
    const next = [card, ...flashcards];
    setFlashcards(next);
    saveStored('icmap_flashcards', next);
    showToast('Custom flashcard saved!');
  }

  function handleDeleteCard(cardId: string) {
    const next = flashcards.filter((c) => c.id !== cardId);
    setFlashcards(next);
    saveStored('icmap_flashcards', next);
  }

  function handleUploadFiles(e: React.ChangeEvent<HTMLInputElement>) {
    const added: UploadedLocalMaterial[] = Array.from(e.target.files || []).map((f) => ({
      name: f.name,
      size: f.size,
      type: f.type,
      date: Date.now()
    }));
    const next = [...uploadedMaterials, ...added];
    setUploadedMaterials(next);
    saveStored('icmap_uploaded_files', next);
    showToast(`Added ${added.length} local file(s).`);
  }

  const navItems: Array<[PortalTab, string, React.ComponentType<any>]> = [
    ['home', 'Dashboard', BarChart3],
    ['read', 'Read & PDF', BookOpen],
    ['flashcards', '5 Daily Cards', Layers],
    ['tests', 'Daily Tests', CheckCircle2],
    ['tutor', 'Urdu Coach', Brain],
    ['videos', 'Videos', Video],
    ['materials', 'Materials', FileText],
    ['past', 'Past Papers', GraduationCap]
  ];

  // Immersive Full-Screen Overlay for Laptop & Mobile Focus
  if (isFullScreen) {
    return (
      <div className="min-h-screen bg-[#050b14] p-4 md:p-6">
        <div className="max-w-6xl mx-auto">
          <div className="flex items-center justify-between gap-3 mb-4 glass rounded-2xl px-4 py-3">
            <div className="text-xs uppercase tracking-widest text-cyan-300 font-semibold">
              Full-Screen Focus Mode •{' '}
              {fullScreenTarget === 'pomodoro'
                ? 'Pomodoro Timer'
                : fullScreenTarget === 'roadmap'
                  ? 'Study Roadmap'
                  : navItems.find((n) => n[0] === tab)?.[1] || 'Study Portal'}
            </div>
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={toggleGlobalTheme}
                aria-label={themeMode === 'midnight' ? 'Switch to High-Contrast Light theme' : 'Switch to Midnight theme'}
                data-testid="fullscreen_theme_toggle"
                className="px-3 py-2 rounded-xl bg-white/10 hover:bg-white/15 text-xs font-semibold flex items-center gap-1.5 min-h-[40px]"
              >
                {themeMode === 'midnight' ? <Sun size={15} /> : <Moon size={15} />}
                {themeMode === 'midnight' ? 'Light' : 'Midnight'}
              </button>
              <button
                type="button"
                onClick={() => toggleFullScreen()}
                className="px-3.5 py-2 rounded-xl bg-white/10 hover:bg-white/15 text-xs font-semibold flex items-center gap-1.5"
              >
                <Minimize2 size={15} /> Exit Full Screen
              </button>
            </div>
          </div>

          {fullScreenTarget === 'pomodoro' ? (
            <PomodoroTimer
              settings={pomodoroSettings}
              onUpdateSettings={(next) => {
                setPomodoroSettings(next);
                saveStored('icmap_pomodoro_cfg', next);
              }}
              onFocusSessionCompleted={(mins) => {
                logStudyMinutes(mins, 'Pomodoro focus block');
              }}
              isFullScreen
              onToggleFullScreen={() => toggleFullScreen()}
            />
          ) : fullScreenTarget === 'roadmap' ? (
            <StudyRoadmap
              completedMilestoneIds={completedMilestones}
              onToggleMilestone={handleToggleMilestone}
              onStartMilestone={(ms) => {
                setFullScreenTarget('active');
                handleStartMilestone(ms);
              }}
              isFullScreen
              onToggleFullScreen={() => toggleFullScreen()}
            />
          ) : tab === 'read' ? (
            <PdfAndReaderView
              selectedMaterial={selectedMaterial}
              onSelectMaterial={setSelectedMaterial}
              readProgressMap={readProgressMap}
              savedNotesMap={savedNotesMap}
              onMarkRead={handleMarkRead}
              onSaveNotes={handleSaveNotes}
              onRunSelectionAi={runSelectionAi}
              onAskTeacherInsideReader={askTeacherInsideReader}
              onLookupWebConcept={lookupWebConceptSummary}
              onSpeakText={speakText}
              onSendToTutor={(prompt, reply) => {
                setChat((prev) => [
                  ...prev,
                  { id: `u_${Date.now()}`, role: 'user', text: prompt },
                  { id: `t_${Date.now() + 1}`, role: 'tutor', text: reply, provider: 'Reader AI' }
                ]);
                setTab('tutor');
              }}
              onGenerateFlashcards={(matId) => {
                const mat = STUDY_MATERIALS.find((m) => m.id === matId);
                if (mat) setSelectedMaterial(mat);
                void generateFlashcardsContent(mat?.readingContent, 5);
              }}
              onLaunchQuiz={(t) => {
                setTopic(t);
                setPreferredTestMode('office_short');
                setTab('tests');
              }}
              videoQuery={videoQuery}
              onChangeVideoQuery={setVideoQuery}
              videos={videos}
              onFindVideos={() => void findVideos()}
              isFullScreen
              onToggleFullScreen={() => toggleFullScreen()}
            />
          ) : tab === 'flashcards' ? (
            <FlashcardDeck
              cards={flashcards}
              todayFiveCardIds={todayFiveCardIds}
              defaultTodayFiveMode={defaultTodayFiveCards}
              onRecordReview={handleFlashcardReview}
              onAddCustomCard={handleAddCustomCard}
              onDeleteCard={handleDeleteCard}
              onGenerateFromMaterial={async (matId, count) => {
                const mat = STUDY_MATERIALS.find((m) => m.id === matId);
                if (mat) setSelectedMaterial(mat);
                await generateFlashcardsContent(mat?.readingContent, count);
              }}
              isGenerating={generatingCards}
              isFullScreen
              onToggleFullScreen={() => toggleFullScreen()}
            />
          ) : tab === 'tests' ? (
            <DailyTestsArena
              weakTopics={weakTopics}
              recentAttempts={testHistory}
              initialMode={preferredTestMode}
              initialPortionFilter={preferredPortionFilter}
              lang={lang}
              isFullScreen
              onToggleFullScreen={() => toggleFullScreen()}
              onGenerateAiTest={generateAiTest}
              onGradeTypedAnswer={gradeTypedAnswer}
              onSpeakText={speakText}
              onTestCompleted={handleTestCompleted}
            />
          ) : (
            renderTutorPanel()
          )}
        </div>
      </div>
    );
  }

  function renderTutorPanel() {
    return (
      <div className="grid lg:grid-cols-[1fr_320px] gap-5">
        <div className="glass rounded-3xl p-5 min-h-[620px] flex flex-col">
          <div className="flex flex-wrap items-center justify-between gap-2 pb-3 border-b border-white/10">
            <div>
              <div className="text-xs text-cyan-300 uppercase font-semibold">
                30-Yr Strategist + 50-Yr OM Master Teacher (Groq + Zero-Key Failover)
              </div>
              <h2 className="text-xl font-bold">Urdu Voice Mentor & Doubt Solver</h2>
            </div>
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() =>
                  setEngineMode((m) =>
                    m === 'auto_free' ? 'pollinations_direct' : 'auto_free'
                  )
                }
                className="px-3 py-1.5 rounded-xl bg-cyan-500/15 text-cyan-100 text-xs font-medium"
              >
                {engineMode === 'auto_free'
                  ? 'Groq + Auto Failover'
                  : 'Pollinations Direct ($0)'}
              </button>
              <button
                type="button"
                onClick={() => {
                  const next = !voiceEnabled;
                  setVoiceEnabled(next);
                  if (!next) stopSpeech();
                }}
                aria-label="Toggle voice readout"
                className="p-2.5 rounded-xl bg-white/5"
              >
                {voiceEnabled ? <Volume2 size={16} /> : <VolumeX size={16} />}
              </button>
              <button
                type="button"
                onClick={() => toggleFullScreen('active')}
                aria-label="Toggle full-screen tutor"
                className="p-2.5 rounded-xl bg-white/5"
              >
                {isFullScreen ? <Minimize2 size={16} /> : <Maximize2 size={16} />}
              </button>
            </div>
          </div>

          <div className="flex-1 overflow-y-auto scrollbar space-y-3 mt-4 pr-1">
            {chat.map((m) => (
              <div
                key={m.id}
                className={`p-4 rounded-2xl max-w-[92%] ${
                  m.role === 'user' ? 'ml-auto bg-cyan-500/15' : 'bg-white/5'
                }`}
              >
                <div className="text-[10px] uppercase text-slate-400 flex items-center justify-between gap-3">
                  <span>{m.role === 'user' ? 'You' : 'Ustad & Strategist'}</span>
                  <div className="flex items-center gap-2">
                    {m.provider && <span className="text-cyan-300">{m.provider}</span>}
                    {m.role === 'tutor' && (
                      <button
                        type="button"
                        onClick={() => speakText(m.text, lang)}
                        aria-label="Listen to tutor answer"
                        className="text-cyan-200 hover:text-white inline-flex items-center gap-1"
                      >
                        <Volume2 size={12} /> Listen
                      </button>
                    )}
                  </div>
                </div>
                <div className="whitespace-pre-wrap text-sm leading-6 mt-1">{m.text}</div>
              </div>
            ))}
          </div>

          {tutorError && (
            <div className="mt-3 p-3 rounded-xl bg-rose-500/15 text-rose-200 text-xs">
              {tutorError}
            </div>
          )}

          <div className="flex gap-2 mt-4">
            <button
              type="button"
              disabled={busy}
              onClick={startVoiceQuestion}
              aria-label="Record voice message question"
              className={`px-3.5 rounded-xl min-w-[48px] flex items-center justify-center transition ${
                listeningVoice
                  ? 'bg-rose-500/35 text-rose-100 animate-pulse'
                  : 'bg-violet-500/20 hover:bg-violet-500/30 text-violet-100'
              }`}
            >
              <Mic size={17} />
            </button>
            <input
              value={question}
              onChange={(e) => setQuestion(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && void askTutor()}
              placeholder={
                listeningVoice
                  ? 'Sun raha hoon — Urdu ya English mein boliyeh...'
                  : 'Ask in Urdu/English by typing or tap Mic...'
              }
              className="flex-1 bg-black/25 border border-white/10 rounded-xl px-4 py-3 text-sm"
            />
            <button
              type="button"
              disabled={busy}
              onClick={() => void askTutor()}
              className="px-4 rounded-xl bg-cyan-500/25 hover:bg-cyan-500/35 min-w-[48px] flex items-center justify-center"
            >
              <Send size={17} />
            </button>
          </div>
        </div>

        {/* Right Column: $0 Wikipedia / Dictionary + Quick Prompts */}
        <div className="space-y-4">
          <div className="glass rounded-3xl p-4 space-y-3">
            <div className="text-xs uppercase tracking-wider text-cyan-300 font-semibold flex items-center gap-1.5">
              <Globe size={14} /> Free Web & Dictionary ($0)
            </div>
            <div className="flex gap-2">
              <input
                value={refQuery}
                onChange={(e) => setRefQuery(e.target.value)}
                placeholder="e.g. Balanced Scorecard or kaizen"
                className="flex-1 bg-black/25 border border-white/10 rounded-xl px-3 py-2 text-xs"
              />
            </div>
            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                disabled={refBusy || !refQuery.trim()}
                onClick={() => void runFreeLookup('wiki')}
                className="py-2 px-2.5 rounded-xl bg-cyan-500/20 text-cyan-100 text-xs font-semibold flex items-center justify-center gap-1"
              >
                <Search size={13} /> Web Concept
              </button>
              <button
                type="button"
                disabled={refBusy || !refQuery.trim()}
                onClick={() => void runFreeLookup('dict')}
                className="py-2 px-2.5 rounded-xl bg-violet-500/20 text-violet-100 text-xs font-semibold flex items-center justify-center gap-1"
              >
                <BookOpen size={13} /> Dictionary
              </button>
            </div>
            {refError && <div className="text-xs text-rose-300">{refError}</div>}
            {refResult && (
              <div className="p-3 rounded-2xl bg-white/5 border border-white/10 space-y-2 text-xs">
                <div className="flex justify-between items-start gap-2">
                  <div>
                    <div className="text-[10px] text-cyan-300 font-semibold">
                      {refResult.sourceName}
                    </div>
                    <div className="font-bold text-sm">{refResult.title}</div>
                  </div>
                  <button type="button" onClick={() => setRefResult(null)}>
                    <X size={14} />
                  </button>
                </div>
                <p className="text-slate-300 line-clamp-5 whitespace-pre-wrap">{refResult.summary}</p>
                <button
                  type="button"
                  onClick={() =>
                    void askTutor(
                      `Is web definition ko asaan Urdu + Finance example + 1 ICMA 5-mark sawal mein tabdeel karein:\n${refResult.title}: ${refResult.summary.slice(0, 900)}`
                    )
                  }
                  className="w-full py-2 rounded-xl bg-cyan-500/25 text-cyan-100 font-semibold"
                >
                  Explain in Urdu + Merit Notes
                </button>
              </div>
            )}
          </div>

          <div className="glass rounded-3xl p-4 space-y-2">
            <h3 className="font-bold text-sm">Instant Teacher Prompts</h3>
            {[
              'Mintzberg 5 organizational parts Urdu + corporate example se samjhao.',
              'Balanced Scorecard perspectives aur 4 drawbacks yaad karwa dein.',
              'Mujhe aaj office mein 2 meetings aa gayi hain, plan compress karein.',
              'Test mein confidence low hai — Merit strategy aur dua/motivation dein.'
            ].map((q) => (
              <button
                key={q}
                type="button"
                onClick={() => void askTutor(q)}
                className="w-full text-left p-3 rounded-xl bg-white/5 hover:bg-white/10 text-xs"
              >
                {q}
              </button>
            ))}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex flex-col">
      {/* Sticky Top Header */}
      <header className="sticky top-0 z-30 glass border-x-0 border-t-0">
        <div className="max-w-7xl mx-auto px-4 py-3 flex justify-between items-center gap-3">
          <button
            type="button"
            onClick={() => setTab('home')}
            className="flex gap-3 items-center text-left"
          >
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-cyan-400 to-violet-500 flex items-center justify-center shrink-0">
              <GraduationCap />
            </div>
            <div>
              <b>
                ICMAP <span className="gradientText">OM MERIT PWA</span>
              </b>
              <div className="text-[10px] text-slate-400">
                5 Dec 2026 Target • OM Only (9-Day Zero-to-Merit) • Laptop & Mobile PWA
              </div>
            </div>
          </button>

          <nav className="hidden lg:flex gap-1">
            {navItems.map(([key, label, IconComp]) => (
              <button
                key={key}
                type="button"
                onClick={() => setTab(key)}
                className={`px-3 py-2 rounded-lg text-xs transition ${
                  tab === key ? 'bg-cyan-400/15 text-cyan-200 font-semibold' : 'text-slate-400 hover:text-white'
                }`}
              >
                <IconComp size={14} className="inline mr-1" />
                {label}
              </button>
            ))}
          </nav>

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={toggleGlobalTheme}
              aria-label={
                themeMode === 'midnight'
                  ? 'Switch to High-Contrast Light theme'
                  : 'Switch to Midnight theme'
              }
              title={
                themeMode === 'midnight'
                  ? 'Switch to High-Contrast Light Theme'
                  : 'Switch to Midnight Theme'
              }
              data-testid="global_theme_toggle"
              className="px-3 py-2 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-xs font-semibold flex items-center gap-1.5 min-h-[40px]"
            >
              {themeMode === 'midnight' ? (
                <>
                  <Sun size={15} className="text-amber-300" />
                  <span className="hidden sm:inline">Light</span>
                </>
              ) : (
                <>
                  <Moon size={15} className="text-cyan-300" />
                  <span className="hidden sm:inline">Midnight</span>
                </>
              )}
            </button>
            {!isStandalonePwa && (
              <button
                type="button"
                onClick={handleInstallPwa}
                className="px-3 py-2 rounded-xl bg-gradient-to-r from-cyan-500/25 to-violet-500/25 border border-cyan-300/30 text-xs font-semibold text-cyan-100 flex items-center gap-1.5"
              >
                <Download size={14} /> Install PWA
              </button>
            )}
            <button
              type="button"
              onClick={() => toggleFullScreen('active')}
              aria-label="Toggle full-screen mode"
              className="p-2 rounded-xl bg-white/5 hover:bg-white/10"
            >
              <Maximize2 size={16} />
            </button>
            <button
              type="button"
              className="lg:hidden p-2 rounded-xl bg-white/5"
              onClick={() => setMobileMenuOpen((v) => !v)}
              aria-label="Toggle menu"
            >
              <Menu size={18} />
            </button>
          </div>
        </div>

        {mobileMenuOpen && (
          <div className="lg:hidden p-3 grid grid-cols-2 sm:grid-cols-4 gap-2 border-t border-white/10">
            {navItems.map(([key, label, IconComp]) => (
              <button
                key={key}
                type="button"
                onClick={() => {
                  setTab(key);
                  setMobileMenuOpen(false);
                }}
                className={`glass p-3 rounded-xl text-xs flex items-center gap-2 ${
                  tab === key ? 'border-cyan-300/40 text-cyan-200' : ''
                }`}
              >
                <IconComp size={14} /> {label}
              </button>
            ))}
          </div>
        )}
      </header>

      {/* Main Workspace Grid */}
      <main className="max-w-7xl mx-auto w-full p-4 md:p-6 flex-1">
        <div className="grid xl:grid-cols-[260px_1fr] gap-5">
          {/* Desktop Course Sidebar */}
          <aside className="hidden xl:block glass rounded-3xl p-4 h-fit sticky top-20 space-y-4">
            <div>
              <div className="text-xs text-cyan-300 uppercase tracking-widest mb-3 font-semibold">
                5 Dec Exam: {dailyPlan.daysToExam}d • {overall}/100 Est.
              </div>
              {portionStats.map((p) => {
                const matchedSubj = SUBJECTS.find((s) => s.code === p.code) || SUBJECTS[0];
                return (
                  <button
                    key={p.code}
                    type="button"
                    onClick={() => setSubject(matchedSubj)}
                    className={`w-full text-left p-3 rounded-2xl mb-2 transition ${
                      subject.code === p.code
                        ? 'bg-cyan-400/12 border border-cyan-300/30'
                        : 'hover:bg-white/5'
                    }`}
                  >
                    <div className="flex justify-between text-xs font-bold">
                      <span>
                        {p.partLabel} <span className="text-amber-300">({p.weightPercent}%)</span>
                      </span>
                      <span className="text-cyan-200">{p.overallPrepPct}%</span>
                    </div>
                    <div className="progress mt-1.5">
                      <span style={{ width: `${p.overallPrepPct}%` }} />
                    </div>
                    <div className="text-[11px] text-slate-400 mt-1">
                      {p.title} • {p.units}
                    </div>
                  </button>
                );
              })}
            </div>

            <div className="border-t border-white/10 pt-4 text-xs space-y-2">
              <div className="text-slate-400">Mentor Language & Voice</div>
              <div className="flex items-center gap-1.5">
                <button
                  type="button"
                  onClick={() => setLang('ur')}
                  className={`px-3 py-2 rounded-lg ${
                    lang === 'ur' ? 'bg-cyan-400/20 text-cyan-200 font-bold' : 'bg-white/5'
                  }`}
                >
                  Urdu
                </button>
                <button
                  type="button"
                  onClick={() => setLang('en')}
                  className={`px-3 py-2 rounded-lg ${
                    lang === 'en' ? 'bg-cyan-400/20 text-cyan-200 font-bold' : 'bg-white/5'
                  }`}
                >
                  English
                </button>
                <button
                  type="button"
                  onClick={() => {
                    const next = !voiceEnabled;
                    setVoiceEnabled(next);
                    if (!next) stopSpeech();
                  }}
                  className="p-2 rounded-lg bg-white/5 ml-auto"
                >
                  {voiceEnabled ? <Volume2 size={14} /> : <VolumeX size={14} />}
                </button>
              </div>
            </div>
          </aside>

          {/* Active Tab Content */}
          <section className="min-w-0 space-y-5">
            {tab === 'home' && (
              <>
                {/* 1. EXECUTIVE MERIT & OFFICIAL SYLLABUS WEIGHTAGE COMMAND CENTER */}
                <ExecutiveDashboard
                  portions={portionStats}
                  daysToExam={dailyPlan.daysToExam}
                  weakTopics={weakTopics}
                  studyMinutesTotal={studyMinutesTotal}
                  dailyStudyMinutes={dailyStudyMinutes}
                  completedMilestonesCount={completedMilestones.length}
                  totalMilestonesCount={ROADMAP_MILESTONES.length}
                  testAttemptsCount={testHistory.length}
                  onLogStudyMinutes={logStudyMinutes}
                  onPracticePortion={(bankFilter) => {
                    setPreferredTestMode('portion_grand');
                    setPreferredPortionFilter(bankFilter);
                    setTab('tests');
                  }}
                  onStudyPortion={(p) => {
                    const matchedMat = STUDY_MATERIALS[0];
                    setSelectedMaterial(matchedMat);
                    setTopic(p.topics[0] || p.title);
                    setTab('read');
                  }}
                  onOpenRepairTopic={(w) => {
                    setTopic(w);
                    setTab('read');
                  }}
                />

                {/* 2. MASTER STRATEGIST BRIEF OF THE DAY + URDU VOICE ASK TEACHER */}
                <DailyBriefAndCoach
                  plan={dailyPlan}
                  weakTopics={weakTopics}
                  lang={lang}
                  onChangeLang={setLang}
                  onChangeEnergyMode={(m) => {
                    setEnergyMode(m);
                    saveStored('icmap_energy_mode', m);
                    showToast(`Routine adapted to ${m.replace('_', ' ')} mode!`);
                  }}
                  onToggleSlot={(slotId) => {
                    const next = completedSlotIds.includes(slotId)
                      ? completedSlotIds.filter((id) => id !== slotId)
                      : [...completedSlotIds, slotId];
                    setCompletedSlotIds(next);
                    saveStored('icmap_daily_slots_done', next);
                  }}
                  onLaunchAction={handleLaunchRoutineAction}
                  onLaunchTodayVideo={(q) => {
                    setVideoQuery(q);
                    setTab('videos');
                    void findVideos(q);
                  }}
                  onSpeakText={speakText}
                  onStopSpeech={stopSpeech}
                  isSpeaking={isSpeaking}
                  onGenerateAiBrief={generateAiDailyBrief}
                  coachMessages={chat}
                  onAskCoach={askTutor}
                  onStartVoiceCoach={startVoiceQuestion}
                  listeningVoice={listeningVoice}
                  coachBusy={busy}
                />

                {/* 3. Pomodoro Timer + Study Roadmap Side-by-Side */}
                <div className="grid lg:grid-cols-[360px_1fr] gap-5 items-start">
                  <PomodoroTimer
                    settings={pomodoroSettings}
                    onUpdateSettings={(next) => {
                      setPomodoroSettings(next);
                      saveStored('icmap_pomodoro_cfg', next);
                    }}
                    onFocusSessionCompleted={(mins) => {
                      logStudyMinutes(mins, 'Pomodoro focus block');
                    }}
                    isFullScreen={false}
                    onToggleFullScreen={() => toggleFullScreen('pomodoro')}
                  />

                  <StudyRoadmap
                    completedMilestoneIds={completedMilestones}
                    onToggleMilestone={handleToggleMilestone}
                    onStartMilestone={handleStartMilestone}
                    isFullScreen={false}
                    onToggleFullScreen={() => toggleFullScreen('roadmap')}
                  />
                </div>
              </>
            )}

            {tab === 'read' && (
              <PdfAndReaderView
                selectedMaterial={selectedMaterial}
                onSelectMaterial={setSelectedMaterial}
                readProgressMap={readProgressMap}
                savedNotesMap={savedNotesMap}
                onMarkRead={handleMarkRead}
                onSaveNotes={handleSaveNotes}
                onRunSelectionAi={runSelectionAi}
                onAskTeacherInsideReader={askTeacherInsideReader}
                onLookupWebConcept={lookupWebConceptSummary}
                onSpeakText={speakText}
                onSendToTutor={(prompt, reply) => {
                  setChat((prev) => [
                    ...prev,
                    { id: `u_${Date.now()}`, role: 'user', text: prompt },
                    { id: `t_${Date.now() + 1}`, role: 'tutor', text: reply, provider: 'Reader AI' }
                  ]);
                  setTab('tutor');
                }}
                onGenerateFlashcards={(matId) => {
                  const mat = STUDY_MATERIALS.find((m) => m.id === matId);
                  if (mat) setSelectedMaterial(mat);
                  void generateFlashcardsContent(mat?.readingContent, 5);
                }}
                onLaunchQuiz={(t) => {
                  setTopic(t);
                  setPreferredTestMode('office_short');
                  setTab('tests');
                }}
                videoQuery={videoQuery}
                onChangeVideoQuery={setVideoQuery}
                videos={videos}
                onFindVideos={() => void findVideos()}
                isFullScreen={false}
                onToggleFullScreen={() => toggleFullScreen('active')}
              />
            )}

            {tab === 'flashcards' && (
              <FlashcardDeck
                cards={flashcards}
                todayFiveCardIds={todayFiveCardIds}
                defaultTodayFiveMode={defaultTodayFiveCards}
                onRecordReview={handleFlashcardReview}
                onAddCustomCard={handleAddCustomCard}
                onDeleteCard={handleDeleteCard}
                onGenerateFromMaterial={async (matId, count) => {
                  const mat = STUDY_MATERIALS.find((m) => m.id === matId);
                  if (mat) setSelectedMaterial(mat);
                  await generateFlashcardsContent(mat?.readingContent, count);
                }}
                isGenerating={generatingCards}
                isFullScreen={false}
                onToggleFullScreen={() => toggleFullScreen('active')}
              />
            )}

            {tab === 'tests' && (
              <DailyTestsArena
                weakTopics={weakTopics}
                recentAttempts={testHistory}
                initialMode={preferredTestMode}
                initialPortionFilter={preferredPortionFilter}
                lang={lang}
                isFullScreen={false}
                onToggleFullScreen={() => toggleFullScreen('active')}
                onGenerateAiTest={generateAiTest}
                onGradeTypedAnswer={gradeTypedAnswer}
                onSpeakText={speakText}
                onTestCompleted={handleTestCompleted}
              />
            )}

            {tab === 'tutor' && renderTutorPanel()}

            {tab === 'videos' && (
              <div className="glass rounded-3xl p-5 space-y-4">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div>
                    <div className="text-xs text-cyan-300 uppercase font-semibold">
                      Recommended Video for Today’s Plan
                    </div>
                    <h2 className="text-xl md:text-2xl font-bold">{dailyPlan.todayVideoQuery}</h2>
                  </div>
                  <button
                    type="button"
                    onClick={() => void findVideos(dailyPlan.todayVideoQuery)}
                    className="px-4 py-2.5 rounded-xl bg-gradient-to-r from-rose-500 to-violet-500 text-xs font-bold flex items-center gap-1.5"
                  >
                    <Play size={14} /> Watch Today’s Lecture
                  </button>
                </div>
                <div className="flex gap-2">
                  <input
                    value={videoQuery}
                    onChange={(e) => setVideoQuery(e.target.value)}
                    className="flex-1 bg-black/25 border border-white/10 rounded-xl p-3 text-sm"
                  />
                  <button
                    type="button"
                    onClick={() => void findVideos()}
                    className="px-4 rounded-xl bg-rose-500/20 text-rose-100 flex items-center gap-1.5 text-xs font-semibold"
                  >
                    <Play size={15} /> Find
                  </button>
                </div>
                <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-4">
                  {videos.map((v) => (
                    <a
                      target="_blank"
                      rel="noreferrer"
                      href={`https://www.youtube.com/watch?v=${v.id}`}
                      key={v.id}
                      className="glass rounded-xl overflow-hidden"
                    >
                      <img src={v.thumbnail} alt={v.title} className="w-full" />
                      <div className="p-3 text-sm">{v.title}</div>
                    </a>
                  ))}
                </div>
              </div>
            )}

            {tab === 'materials' && (
              <div className="grid lg:grid-cols-2 gap-5">
                <div className="glass rounded-3xl p-5 space-y-4">
                  <h2 className="text-xl font-bold">Bundled Books & Local Files</h2>
                  <div className="space-y-2">
                    {STUDY_MATERIALS.map((mat) => (
                      <div
                        key={mat.id}
                        className="p-3.5 rounded-2xl bg-white/5 border border-white/10 flex items-center justify-between gap-3"
                      >
                        <div className="min-w-0">
                          <div className="font-semibold text-sm truncate">{mat.title}</div>
                          <div className="text-xs text-slate-400">{mat.category}</div>
                        </div>
                        <button
                          type="button"
                          onClick={() => {
                            setSelectedMaterial(mat);
                            setTab('read');
                          }}
                          className="px-3 py-2 rounded-xl bg-cyan-500/20 text-cyan-100 text-xs font-semibold shrink-0"
                        >
                          Open
                        </button>
                      </div>
                    ))}
                  </div>
                  <label className="flex flex-col items-center justify-center p-6 border border-dashed border-cyan-300/25 rounded-2xl bg-cyan-400/5 cursor-pointer">
                    <Upload size={20} />
                    <span className="mt-2 text-xs">Attach additional local study files</span>
                    <input
                      type="file"
                      multiple
                      accept=".pdf,.doc,.docx,.ppt,.pptx,.txt"
                      className="hidden"
                      onChange={handleUploadFiles}
                    />
                  </label>
                  {uploadedMaterials.map((m, idx) => (
                    <div
                      key={`${m.name}_${idx}`}
                      className="p-3 rounded-xl bg-white/5 flex justify-between text-xs"
                    >
                      <span>{m.name}</span>
                      <span className="text-slate-400">{Math.round(m.size / 1024)} KB</span>
                    </div>
                  ))}
                </div>

                <div className="glass rounded-3xl p-5 space-y-3">
                  <h3 className="font-bold text-lg">Your OM Google Drive Folders & Docs</h3>
                  <div className="space-y-2">
                    {[
                      {
                        label: 'OM Core Study Folder 1',
                        url: 'https://drive.google.com/drive/folders/1JRAADz_hSw7MLr4_Pl6fuhuyRhlyt3ex'
                      },
                      {
                        label: 'OM Supplementary Notes Folder 2',
                        url: 'https://drive.google.com/drive/folders/1EdfluOCsZ_NUiRZE_oJRT9eXiK4d4aT3'
                      },
                      {
                        label: 'Master OM Study Guide (Google Doc)',
                        url: 'https://docs.google.com/document/d/1wZP3HFOlBIjIZJzw82Z_-yT7yeuuYfZYh3IVE3G3NJQ/edit?usp=drivesdk'
                      },
                      {
                        label: 'OM Practice Banks & Past Sets Folder',
                        url: 'https://drive.google.com/drive/folders/1cSJry6g8mBLEz0slwzLHyz-UyiGkvfXM'
                      },
                      {
                        label: 'OM Lecture Videos & Clips Folder',
                        url: 'https://drive.google.com/drive/folders/1trJJR26p772Twd0O7hFuzl-uRCtqDRzx'
                      }
                    ].map((d) => (
                      <a
                        key={d.url}
                        href={d.url}
                        target="_blank"
                        rel="noreferrer"
                        className="p-3 rounded-xl bg-white/5 hover:bg-cyan-500/15 border border-white/10 flex items-center justify-between gap-2 text-xs font-medium text-cyan-100"
                      >
                        <span className="truncate">{d.label}</span>
                        <ExternalLink size={14} className="shrink-0 text-cyan-300" />
                      </a>
                    ))}
                  </div>
                  <div className="pt-2 border-t border-white/10 space-y-2">
                    <div className="text-xs text-slate-400">Custom Google Drive / Doc URL</div>
                    <input
                      value={driveLink}
                      onChange={(e) => {
                        setDriveLink(e.target.value);
                        saveStored('icmap_drive_link', e.target.value);
                      }}
                      placeholder="Paste Google Drive folder or file link"
                      className="w-full bg-black/25 border border-white/10 rounded-xl p-3 text-sm"
                    />
                    <button
                      type="button"
                      onClick={() => driveLink && window.open(driveLink, '_blank')}
                      className="px-4 py-2.5 rounded-xl bg-cyan-500/20 text-cyan-100 text-xs font-semibold inline-flex items-center gap-1.5"
                    >
                      Open Custom Link <ExternalLink size={14} />
                    </button>
                  </div>
                  <p className="text-xs text-slate-400 pt-1">
                    Works offline on laptop & mobile once installed as a PWA. Your bundled OM
                    Textbook & Past Papers PDFs are cached automatically.
                  </p>
                </div>
              </div>
            )}

            {tab === 'past' && (
              <div className="glass rounded-3xl p-5 md:p-6 space-y-4">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <div className="text-xs text-cyan-300 uppercase font-semibold">
                      ICMA ML-1 Past Paper & Multi-Body Benchmark Lab
                    </div>
                    <h2 className="text-xl md:text-2xl font-bold">
                      Past Papers Q.1–Q.10 + Full 60/40 Mocks
                    </h2>
                  </div>
                  <button
                    type="button"
                    onClick={() => {
                      setSelectedMaterial(STUDY_MATERIALS[1]);
                      setTab('read');
                    }}
                    className="px-4 py-2.5 rounded-xl bg-gradient-to-r from-cyan-500 to-violet-500 text-xs font-semibold"
                  >
                    Open Past Papers PDF & Solutions
                  </button>
                </div>
                <div className="grid md:grid-cols-3 gap-4">
                  {[
                    ['01', '5-Mark Suggested Solutions', 'Study all 10 official ML-1 past questions inside Read & PDF view with Urdu explanations.'],
                    ['02', 'Test of the Day & Office Short', 'Attempt ICMA 60% MCQ / 40% Typing papers auto-calibrated to your weak topics.'],
                    ['03', 'Full-Course Merit Mocks', 'Run Weekly Grand, Portion Grand, Half-Book & 3 Final Full-Course Mocks before Dec 5.']
                  ].map(([num, title, desc]) => (
                    <div className="p-5 rounded-2xl bg-white/5 border border-white/10" key={num}>
                      <div className="text-cyan-300 font-bold">{num}</div>
                      <h3 className="font-semibold mt-2 text-sm">{title}</h3>
                      <p className="text-xs text-slate-400 mt-1.5">{desc}</p>
                    </div>
                  ))}
                </div>
                <div className="flex flex-wrap gap-3">
                  <button
                    type="button"
                    onClick={() => {
                      setPreferredTestMode('full_course_mock');
                      setTab('tests');
                    }}
                    className="px-4 py-3 rounded-xl bg-violet-500/20 hover:bg-violet-500/30 text-xs font-semibold"
                  >
                    Launch Full-Course 60/40 Merit Mock
                  </button>
                  <button
                    type="button"
                    onClick={() => void generateFlashcardsContent(STUDY_MATERIALS[1].readingContent, 5)}
                    className="px-4 py-3 rounded-xl bg-cyan-500/20 hover:bg-cyan-500/30 text-xs font-semibold"
                  >
                    Create 5 Cards from Past Papers
                  </button>
                </div>
              </div>
            )}
          </section>
        </div>
      </main>

      {/* Install PWA Instructions Modal */}
      {showInstallHelp && (
        <div className="fixed inset-0 z-50 bg-black/70 flex items-center justify-center p-4">
          <div className="glass rounded-3xl p-6 max-w-md w-full space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="font-bold text-lg">Install as App (Laptop & Phone)</h3>
              <button type="button" onClick={() => setShowInstallHelp(false)}>
                <X size={18} />
              </button>
            </div>
            <ul className="text-xs text-slate-300 space-y-2 leading-5">
              <li>
                <strong>Windows / Mac / Linux (Chrome or Edge):</strong> Click the{' '}
                <strong>Install</strong> icon in the address bar (or ⋮ Menu →{' '}
                <em>Cast, save, and share → Install page as app</em>).
              </li>
              <li>
                <strong>Android (Chrome):</strong> Tap ⋮ → <strong>Add to Home screen / Install app</strong>.
              </li>
              <li>
                <strong>iPhone / iPad (Safari):</strong> Tap <strong>Share</strong> →{' '}
                <strong>Add to Home Screen</strong>.
              </li>
            </ul>
            <button
              type="button"
              onClick={() => setShowInstallHelp(false)}
              className="w-full py-2.5 rounded-xl bg-cyan-500/25 text-cyan-100 text-xs font-semibold"
            >
              Got it
            </button>
          </div>
        </div>
      )}

      {toastMsg && (
        <div className="fixed bottom-5 right-5 z-50 glass px-4 py-3 rounded-2xl border-cyan-300/40 text-xs text-cyan-100 shadow-xl">
          {toastMsg}
        </div>
      )}

      <footer className="max-w-7xl mx-auto w-full px-4 pb-6 pt-2 text-xs text-slate-500 flex justify-between">
        <span>ICMAP OM Merit Strategist PWA • {OM_MCQ_QUESTIONS.length} MCQs + 10 Past-Paper Typing Qs</span>
        <span>{busy ? 'Teacher thinking…' : 'Ready for Dec 5 Merit'}</span>
      </footer>
    </div>
  );
}
