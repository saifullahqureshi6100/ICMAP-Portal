import React, { useEffect, useMemo, useState } from 'react';
import {
  BarChart3,
  BookOpen,
  Brain,
  Calculator,
  CalendarDays,
  CheckCircle2,
  ChevronRight,
  Clock,
  Download,
  ExternalLink,
  FileText,
  Globe,
  GraduationCap,
  Layers,
  Maximize2,
  Menu,
  Minimize2,
  PenLine,
  Play,
  Search,
  Send,
  Sparkles,
  Target,
  Upload,
  Video,
  Volume2,
  VolumeX,
  X
} from 'lucide-react';
import {
  Bar,
  BarChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from 'recharts';
import { FlashcardDeck } from './components/FlashcardDeck';
import { PdfAndReaderView } from './components/PdfAndReaderView';
import { PomodoroTimer } from './components/PomodoroTimer';
import { StudyRoadmap } from './components/StudyRoadmap';
import {
  OM_MCQ_QUESTIONS,
  STARTER_FLASHCARDS,
  STUDY_MATERIALS,
  SUBJECTS
} from './data/icmapData';
import {
  callFreeAiTutor,
  fetchDictionaryWord,
  fetchWikipediaConcept,
  parseMcqArrayFromAi
} from './lib/freeAiClient';
import {
  AiEngineMode,
  FlashcardItem,
  FreeReferenceItem,
  McqQuestionItem,
  PomodoroSettings,
  PortalTab,
  RoadmapMilestone,
  StudyMaterialItem,
  SubjectInfo,
  TutorChatItem,
  TutorLang,
  UploadedLocalMaterial
} from './types';

interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>;
}

function speakText(text: string, lang: TutorLang) {
  if (typeof window === 'undefined' || !window.speechSynthesis) return;
  window.speechSynthesis.cancel();
  const utterance = new SpeechSynthesisUtterance(String(text).slice(0, 1400));
  utterance.lang = lang === 'ur' ? 'ur-PK' : 'en-US';
  utterance.rate = 0.94;
  window.speechSynthesis.speak(utterance);
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

export default function App() {
  const [tab, setTab] = useState<PortalTab>('home');
  const [subject, setSubject] = useState<SubjectInfo>(SUBJECTS[0]);
  const [topic, setTopic] = useState<string>(SUBJECTS[0].weak[0]);
  const [selectedMaterial, setSelectedMaterial] = useState<StudyMaterialItem>(STUDY_MATERIALS[0]);
  const [lang, setLang] = useState<TutorLang>('en');
  const [voiceEnabled, setVoiceEnabled] = useState(false);
  const [engineMode, setEngineMode] = useState<AiEngineMode>('auto_free');

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
      text: 'Welcome to ICMAP Smart Tutor PWA! Ask me anything on M3 Organizational Management, Past Papers, Accounting, or Taxation — works free with zero API key required.',
      provider: 'Free AI Ready'
    }
  ]);
  const [busy, setBusy] = useState(false);
  const [tutorError, setTutorError] = useState('');
  const [refQuery, setRefQuery] = useState('');
  const [refResult, setRefResult] = useState<FreeReferenceItem | null>(null);
  const [refBusy, setRefBusy] = useState(false);
  const [refError, setRefError] = useState('');

  // Quizzes & Flashcards
  const [quiz, setQuiz] = useState<McqQuestionItem[]>(OM_MCQ_QUESTIONS);
  const [qi, setQi] = useState(0);
  const [selectedAnswers, setSelectedAnswers] = useState<Record<number, number>>({});
  const [quizDone, setQuizDone] = useState(false);
  const [flashcards, setFlashcards] = useState<FlashcardItem[]>(() =>
    loadStored('icmap_flashcards', STARTER_FLASHCARDS)
  );
  const [generatingCards, setGeneratingCards] = useState(false);

  // Videos & Materials
  const [videos, setVideos] = useState<
    Array<{ id: string; title: string; channel: string; thumbnail: string }>
  >([]);
  const [videoQuery, setVideoQuery] = useState('Balanced Scorecard Mintzberg ICMA OM');
  const [uploadedMaterials, setUploadedMaterials] = useState<UploadedLocalMaterial[]>(() =>
    loadStored('icmap_uploaded_files', [])
  );
  const [driveLink, setDriveLink] = useState<string>(() =>
    loadStored('icmap_drive_link', 'https://drive.google.com/drive/folders/1JRAADz_hSw7MLr4_Pl6fuhuyRhlyt3ex')
  );
  const [toastMsg, setToastMsg] = useState('');

  useEffect(() => {
    setTopic(subject.weak[0] || 'Core Concepts');
    setVideoQuery(subject.weak[0] || subject.name);
  }, [subject]);

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
    window.addEventListener('beforeinstallprompt', onBeforeInstall);
    document.addEventListener('fullscreenchange', onFullscreenChange);
    return () => {
      window.removeEventListener('beforeinstallprompt', onBeforeInstall);
      document.removeEventListener('fullscreenchange', onFullscreenChange);
    };
  }, []);

  const overall = useMemo(
    () => Math.round(SUBJECTS.reduce((acc, s) => acc + s.progress, 0) / SUBJECTS.length),
    []
  );

  const quizCorrectCount = useMemo(
    () => quiz.filter((q, idx) => selectedAnswers[idx] === q.answer).length,
    [quiz, selectedAnswers]
  );

  function showToast(msg: string) {
    setToastMsg(msg);
    window.setTimeout(() => setToastMsg((cur) => (cur === msg ? '' : cur)), 3400);
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

  async function askTutor(customPrompt?: string) {
    const q = (customPrompt ?? question).trim();
    if (!q || busy) return;
    setTutorError('');
    setChat((prev) => [...prev, { id: `u_${Date.now()}`, role: 'user', text: q }]);
    if (!customPrompt) setQuestion('');
    setBusy(true);
    try {
      const res = await callFreeAiTutor({
        action: 'chat',
        subject: subject.code,
        topic,
        question: q,
        language: lang,
        context: `${selectedMaterial.title}\n${uploadedMaterials.map((m) => m.name).join(', ')}`,
        engineMode
      });
      setChat((prev) => [
        ...prev,
        { id: `t_${Date.now()}`, role: 'tutor', text: res.text, provider: res.provider }
      ]);
      if (voiceEnabled) speakText(res.text, lang);
    } catch (e: any) {
      setTutorError(e?.message || 'Could not reach free AI endpoint.');
    } finally {
      setBusy(false);
    }
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

  async function generateContent(
    action: 'mcq' | 'numerical' | 'typing' | 'flashcards' | 'plan',
    sourceContext?: string
  ) {
    setBusy(true);
    if (action === 'flashcards') setGeneratingCards(true);
    setTutorError('');
    try {
      const res = await callFreeAiTutor({
        action,
        subject: subject.code,
        topic,
        count: 6,
        language: lang,
        context: sourceContext || selectedMaterial.readingContent,
        engineMode
      });

      if (action === 'mcq' || action === 'numerical') {
        const parsedQuestions = parseMcqArrayFromAi(res.parsed, `ai_${action}`);
        if (parsedQuestions.length > 0) {
          setQuiz(parsedQuestions);
          setQi(0);
          setSelectedAnswers({});
          setQuizDone(false);
          setTab('tests');
          showToast(`Generated ${parsedQuestions.length} ${action.toUpperCase()} questions!`);
        } else {
          setChat((prev) => [
            ...prev,
            { id: `t_${Date.now()}`, role: 'tutor', text: res.text, provider: res.provider }
          ]);
          setTab('tutor');
        }
      } else if (action === 'flashcards') {
        const rawCards = Array.isArray(res.parsed?.cards) ? res.parsed.cards : [];
        const created: FlashcardItem[] = rawCards
          .filter((c: any) => c?.front && c?.back)
          .map((c: any, i: number) => ({
            id: `ai_fc_${Date.now()}_${i}`,
            deck: selectedMaterial.title,
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
          setTab('flashcards');
          showToast(`Created ${created.length} AI flashcards from ${selectedMaterial.title}!`);
        } else {
          throw new Error('Could not parse AI flashcard JSON. Please try again.');
        }
      } else if (action === 'plan') {
        const sessions = Array.isArray(res.parsed?.sessions) ? res.parsed.sessions : [];
        const planText = sessions.length
          ? `Adaptive Study Plan (${topic}):\n` +
            sessions
              .map(
                (s: any, idx: number) =>
                  `${idx + 1}. ${s.title} (${s.minutes || 35}m) — ${s.task}`
              )
              .join('\n')
          : res.text;
        setChat((prev) => [
          ...prev,
          { id: `p_${Date.now()}`, role: 'tutor', text: planText, provider: res.provider }
        ]);
        setTab('tutor');
      } else {
        setChat((prev) => [
          ...prev,
          { id: `ty_${Date.now()}`, role: 'tutor', text: res.text, provider: res.provider }
        ]);
        setTab('tutor');
      }
    } catch (e: any) {
      setTutorError(e?.message || 'AI generation failed.');
      showToast(e?.message || 'AI generation failed.');
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

  async function findVideos() {
    setBusy(true);
    try {
      const r = await fetch(`/api/youtube?q=${encodeURIComponent(videoQuery)}`);
      if (r.ok) {
        const d = await r.json();
        setVideos(d.items || []);
        if (!(d.items || []).length && d.searchUrl) {
          window.open(d.searchUrl, '_blank');
        }
        return;
      }
    } catch {
      // Static fallback opens clean YouTube search directly
    } finally {
      setBusy(false);
    }
    window.open(
      `https://www.youtube.com/results?search_query=${encodeURIComponent(videoQuery)}`,
      '_blank'
    );
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
    ['flashcards', 'Flashcards', Layers],
    ['tests', 'MCQs', CheckCircle2],
    ['tutor', 'AI Tutor', Brain],
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
            <button
              type="button"
              onClick={() => toggleFullScreen()}
              className="px-3.5 py-2 rounded-xl bg-white/10 hover:bg-white/15 text-xs font-semibold flex items-center gap-1.5"
            >
              <Minimize2 size={15} /> Exit Full Screen
            </button>
          </div>

          {fullScreenTarget === 'pomodoro' ? (
            <PomodoroTimer
              settings={pomodoroSettings}
              onUpdateSettings={(next) => {
                setPomodoroSettings(next);
                saveStored('icmap_pomodoro_cfg', next);
              }}
              onFocusSessionCompleted={(mins) => {
                const updated = studyMinutesTotal + mins;
                setStudyMinutesTotal(updated);
                saveStored('icmap_study_minutes', updated);
                showToast(`Focus block complete! Logged ${mins}m.`);
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
              onSendToTutor={(prompt, reply) => {
                setChat((prev) => [
                  ...prev,
                  { id: `u_${Date.now()}`, role: 'user', text: prompt },
                  { id: `t_${Date.now() + 1}`, role: 'tutor', text: reply, provider: 'Selection AI' }
                ]);
                setTab('tutor');
              }}
              onGenerateFlashcards={(matId) => {
                const mat = STUDY_MATERIALS.find((m) => m.id === matId);
                if (mat) setSelectedMaterial(mat);
                generateContent('flashcards', mat?.readingContent);
              }}
              onLaunchQuiz={(t) => {
                setTopic(t);
                setTab('tests');
              }}
              videoQuery={videoQuery}
              onChangeVideoQuery={setVideoQuery}
              videos={videos}
              onFindVideos={findVideos}
              isFullScreen
              onToggleFullScreen={() => toggleFullScreen()}
            />
          ) : tab === 'flashcards' ? (
            <FlashcardDeck
              cards={flashcards}
              onRecordReview={handleFlashcardReview}
              onAddCustomCard={handleAddCustomCard}
              onDeleteCard={handleDeleteCard}
              onGenerateFromMaterial={async (matId, count) => {
                const mat = STUDY_MATERIALS.find((m) => m.id === matId);
                if (mat) setSelectedMaterial(mat);
                await generateContent('flashcards', mat?.readingContent);
                void count;
              }}
              isGenerating={generatingCards}
              isFullScreen
              onToggleFullScreen={() => toggleFullScreen()}
            />
          ) : tab === 'tests' ? (
            renderTestsPanel()
          ) : (
            renderTutorPanel()
          )}
        </div>
      </div>
    );
  }

  function renderTestsPanel() {
    return (
      <div className="glass rounded-3xl p-5 md:p-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <div className="text-xs text-cyan-300 uppercase tracking-widest font-semibold">
              ICMA Active Test Arena
            </div>
            <h2 className="text-xl md:text-2xl font-bold mt-1">
              {quizDone
                ? `Test Complete: ${quizCorrectCount} / ${quiz.length}`
                : quiz[qi]?.question}
            </h2>
          </div>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => {
                setQuiz(OM_MCQ_QUESTIONS);
                setQi(0);
                setSelectedAnswers({});
                setQuizDone(false);
              }}
              className="glass px-3 py-2 rounded-xl text-xs min-h-[40px]"
            >
              OM Bank (12)
            </button>
            <button
              type="button"
              disabled={busy}
              onClick={() => generateContent('mcq')}
              className="px-3 py-2 rounded-xl bg-cyan-500/20 text-cyan-100 text-xs font-semibold min-h-[40px]"
            >
              AI MCQs
            </button>
            <button
              type="button"
              disabled={busy}
              onClick={() => generateContent('numerical')}
              className="glass px-3 py-2 rounded-xl text-xs min-h-[40px]"
            >
              <Calculator size={13} className="inline mr-1" /> Numericals
            </button>
            <button
              type="button"
              disabled={busy}
              onClick={() => generateContent('typing')}
              className="glass px-3 py-2 rounded-xl text-xs min-h-[40px]"
            >
              <PenLine size={13} className="inline mr-1" /> 5-Mark Drill
            </button>
            <button
              type="button"
              onClick={() => toggleFullScreen('active')}
              aria-label="Toggle full-screen quiz"
              className="glass p-2.5 rounded-xl min-w-[40px] min-h-[40px]"
            >
              {isFullScreen ? <Minimize2 size={15} /> : <Maximize2 size={15} />}
            </button>
          </div>
        </div>

        {quizDone ? (
          <div className="py-10 space-y-5">
            <div className="text-center">
              <CheckCircle2 size={52} className="mx-auto text-emerald-300" />
              <div className="text-3xl font-extrabold mt-3">
                {quizCorrectCount} / {quiz.length} (
                {quiz.length ? Math.round((quizCorrectCount / quiz.length) * 100) : 0}%)
              </div>
              <button
                type="button"
                onClick={() => {
                  setQi(0);
                  setSelectedAnswers({});
                  setQuizDone(false);
                }}
                className="mt-4 px-5 py-2.5 rounded-xl bg-cyan-500/25 text-cyan-100 text-xs font-semibold"
              >
                Retake Quiz
              </button>
            </div>
            <div className="space-y-3">
              {quiz.map((q, idx) => {
                const picked = selectedAnswers[idx];
                const isRight = picked === q.answer;
                return (
                  <div
                    key={q.id}
                    className={`p-4 rounded-2xl border text-xs space-y-1.5 ${
                      isRight
                        ? 'bg-emerald-500/10 border-emerald-400/25'
                        : 'bg-rose-500/10 border-rose-400/25'
                    }`}
                  >
                    <div className="font-bold text-sm">
                      {idx + 1}. {q.question}
                    </div>
                    <div className="text-slate-300">
                      Correct: <span className="text-emerald-200">{q.options[q.answer]}</span>
                    </div>
                    <div className="text-slate-400">{q.explanation}</div>
                  </div>
                );
              })}
            </div>
          </div>
        ) : (
          <div className="mt-6">
            <div className="text-xs text-slate-400 mb-3">
              Question {qi + 1} / {quiz.length}
            </div>
            <div className="grid gap-3">
              {(quiz[qi]?.options || []).map((opt, idx) => (
                <button
                  key={idx}
                  type="button"
                  onClick={() => {
                    setSelectedAnswers((prev) => ({ ...prev, [qi]: idx }));
                    if (qi + 1 < quiz.length) {
                      setQi(qi + 1);
                    } else {
                      setQuizDone(true);
                    }
                  }}
                  className="text-left p-4 rounded-xl bg-white/5 hover:bg-cyan-400/12 border border-white/10 transition min-h-[48px]"
                >
                  <b>{String.fromCharCode(65 + idx)}.</b> {opt}
                </button>
              ))}
            </div>
          </div>
        )}
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
                $0 Free AI Study Tutor
              </div>
              <h2 className="text-xl font-bold">Ask Tutor</h2>
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
                  ? 'Auto Free AI ($0)'
                  : 'Pollinations Direct ($0)'}
              </button>
              <button
                type="button"
                onClick={() => {
                  const next = !voiceEnabled;
                  setVoiceEnabled(next);
                  if (!next && window.speechSynthesis) window.speechSynthesis.cancel();
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
                  <span>{m.role === 'user' ? 'You' : 'Tutor'}</span>
                  {m.provider && <span className="text-cyan-300">{m.provider}</span>}
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
            <input
              value={question}
              onChange={(e) => setQuestion(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && askTutor()}
              placeholder="Ask anything on OM M3, Past Papers, FACR, or BT..."
              className="flex-1 bg-black/25 border border-white/10 rounded-xl px-4 py-3 text-sm"
            />
            <button
              type="button"
              disabled={busy}
              onClick={() => askTutor()}
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
              <Globe size={14} /> Free Wiki & Dictionary ($0 • No Key)
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
                onClick={() => runFreeLookup('wiki')}
                className="py-2 px-2.5 rounded-xl bg-cyan-500/20 text-cyan-100 text-xs font-semibold flex items-center justify-center gap-1"
              >
                <Search size={13} /> Wiki Concept
              </button>
              <button
                type="button"
                disabled={refBusy || !refQuery.trim()}
                onClick={() => runFreeLookup('dict')}
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
                    askTutor(
                      `Turn this concept into concise ICMA 5-mark exam bullet notes + 1 practice question:\n${refResult.title}: ${refResult.summary.slice(0, 900)}`
                    )
                  }
                  className="w-full py-2 rounded-xl bg-cyan-500/25 text-cyan-100 font-semibold"
                >
                  Convert to Exam Notes
                </button>
              </div>
            )}
          </div>

          <div className="glass rounded-3xl p-4 space-y-2">
            <h3 className="font-bold text-sm">Quick Tutor Actions</h3>
            {[
              'Explain Mintzberg 5 organizational parts step by step.',
              'Summarize Balanced Scorecard perspectives & 4 drawbacks.',
              'Give me a 5-mark ICMA OM past paper question and grade me.',
              'Compare Herzberg Hygiene factors vs Motivators.'
            ].map((q) => (
              <button
                key={q}
                type="button"
                onClick={() => setQuestion(q)}
                className="w-full text-left p-3 rounded-xl bg-white/5 hover:bg-white/10 text-xs"
              >
                {q}
              </button>
            ))}
            <button
              type="button"
              disabled={busy}
              onClick={() => generateContent('plan')}
              className="w-full mt-2 p-3 rounded-xl bg-violet-500/20 hover:bg-violet-500/30 text-xs font-semibold"
            >
              Create 5-Step Adaptive Study Plan
            </button>
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
                ICMAP <span className="gradientText">SMART TUTOR PWA</span>
              </b>
              <div className="text-[10px] text-slate-400">
                Laptop • Tablet • Mobile Installable Web App
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
                Courses & Syllabus
              </div>
              {SUBJECTS.map((s) => (
                <button
                  key={s.code}
                  type="button"
                  onClick={() => setSubject(s)}
                  className={`w-full text-left p-3 rounded-2xl mb-2 transition ${
                    subject.code === s.code ? 'bg-cyan-400/12 border border-cyan-300/30' : 'hover:bg-white/5'
                  }`}
                >
                  <div className="flex justify-between text-sm font-bold">
                    <span>{s.code}</span>
                    <span>{s.progress}%</span>
                  </div>
                  <div className="progress mt-2">
                    <span style={{ width: `${s.progress}%` }} />
                  </div>
                  <div className="text-[11px] text-slate-400 mt-1">{s.name}</div>
                </button>
              ))}
            </div>

            <div className="border-t border-white/10 pt-4 text-xs space-y-2">
              <div className="text-slate-400">Tutor Language & Voice</div>
              <div className="flex items-center gap-1.5">
                <button
                  type="button"
                  onClick={() => setLang('en')}
                  className={`px-3 py-2 rounded-lg ${
                    lang === 'en' ? 'bg-cyan-400/20 text-cyan-200' : 'bg-white/5'
                  }`}
                >
                  English
                </button>
                <button
                  type="button"
                  onClick={() => setLang('ur')}
                  className={`px-3 py-2 rounded-lg ${
                    lang === 'ur' ? 'bg-cyan-400/20 text-cyan-200' : 'bg-white/5'
                  }`}
                >
                  Urdu
                </button>
                <button
                  type="button"
                  onClick={() => {
                    const next = !voiceEnabled;
                    setVoiceEnabled(next);
                    if (!next && window.speechSynthesis) window.speechSynthesis.cancel();
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
            {/* Mobile Subject Strip */}
            <div className="xl:hidden flex gap-2 overflow-x-auto pb-1">
              {SUBJECTS.map((s) => (
                <button
                  key={s.code}
                  type="button"
                  onClick={() => setSubject(s)}
                  className={`glass px-3.5 py-2 rounded-xl text-xs whitespace-nowrap ${
                    subject.code === s.code ? 'border-cyan-300/40 text-cyan-200' : ''
                  }`}
                >
                  {s.code} {s.progress}%
                </button>
              ))}
            </div>

            {tab === 'home' && (
              <>
                <div className="glass rounded-3xl p-6 md:p-8 relative overflow-hidden">
                  <div className="absolute -right-20 -top-20 w-64 h-64 bg-cyan-400/10 blur-3xl rounded-full" />
                  <div className="relative">
                    <div className="text-cyan-300 text-xs uppercase tracking-[.2em] font-semibold">
                      Personal Tutor Command Center • PWA
                    </div>
                    <h1 className="text-2xl md:text-4xl font-extrabold mt-2">
                      Study smarter on laptop & phone.{' '}
                      <span className="gradientText">Recall. Practice. Improve.</span>
                    </h1>
                    <p className="text-slate-300 text-sm mt-3 max-w-2xl">
                      Includes your M3 Organizational Management textbook & ML-1 Past Papers PDFs,
                      configurable Pomodoro timer, study roadmap, 3D shuffle flashcards, and free
                      zero-key AI tutor.
                    </p>
                    <div className="flex flex-wrap gap-3 mt-5">
                      <button
                        type="button"
                        onClick={() => {
                          setSelectedMaterial(STUDY_MATERIALS[0]);
                          setTab('read');
                        }}
                        className="px-5 py-3 rounded-xl bg-gradient-to-r from-cyan-500 to-violet-500 font-semibold text-sm flex items-center gap-1.5"
                      >
                        Open M3 OM Book <ChevronRight size={17} />
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          setSelectedMaterial(STUDY_MATERIALS[1]);
                          setTab('read');
                        }}
                        className="px-5 py-3 rounded-xl glass text-sm font-medium"
                      >
                        Open Past Papers
                      </button>
                      <button
                        type="button"
                        onClick={() => setTab('flashcards')}
                        className="px-5 py-3 rounded-xl glass text-sm font-medium"
                      >
                        Shuffle Flashcards
                      </button>
                    </div>
                  </div>
                </div>

                {/* Metrics Row */}
                <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
                  {[
                    ['Overall Readiness', `${overall}%`, Target],
                    ['Focus Logged', `${studyMinutesTotal}m`, Clock],
                    ['Milestones Done', `${completedMilestones.length}/5`, CalendarDays],
                    ['Weak Focus Areas', String(subject.weak.length), Brain]
                  ].map(([label, val, IconComp]: any) => (
                    <div className="glass rounded-2xl p-4" key={label}>
                      <IconComp size={18} className="text-cyan-300" />
                      <div className="text-2xl font-bold mt-2">{val}</div>
                      <div className="text-xs text-slate-400">{label}</div>
                    </div>
                  ))}
                </div>

                {/* Pomodoro Timer + Study Roadmap Side-by-Side on Laptop */}
                <div className="grid lg:grid-cols-[360px_1fr] gap-5 items-start">
                  <PomodoroTimer
                    settings={pomodoroSettings}
                    onUpdateSettings={(next) => {
                      setPomodoroSettings(next);
                      saveStored('icmap_pomodoro_cfg', next);
                    }}
                    onFocusSessionCompleted={(mins) => {
                      const updated = studyMinutesTotal + mins;
                      setStudyMinutesTotal(updated);
                      saveStored('icmap_study_minutes', updated);
                      showToast(`Focus session completed! +${mins} minutes logged.`);
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

                {/* Analytics + Weak Topics */}
                <div className="grid lg:grid-cols-2 gap-5">
                  <div className="glass rounded-3xl p-5">
                    <h3 className="font-bold">Subject Readiness</h3>
                    <div className="h-56 mt-4">
                      <ResponsiveContainer width="100%" height="100%">
                        <BarChart data={SUBJECTS}>
                          <XAxis dataKey="code" stroke="#6b8299" />
                          <YAxis stroke="#6b8299" />
                          <Tooltip />
                          <Bar dataKey="progress" fill="#22d3ee" radius={[6, 6, 0, 0]} />
                        </BarChart>
                      </ResponsiveContainer>
                    </div>
                  </div>
                  <div className="glass rounded-3xl p-5">
                    <h3 className="font-bold">Priority Revision Topics</h3>
                    <div className="space-y-2 mt-4">
                      {SUBJECTS.flatMap((s) => s.weak.map((w) => ({ s, w }))).slice(0, 6).map((item) => (
                        <button
                          key={item.s.code + item.w}
                          type="button"
                          onClick={() => {
                            setSubject(item.s);
                            setTopic(item.w);
                            setTab('read');
                          }}
                          className="w-full p-3 rounded-xl bg-white/5 hover:bg-cyan-400/10 text-left flex justify-between items-center text-sm"
                        >
                          <span>
                            <b>{item.s.code}</b> <span className="text-slate-300">{item.w}</span>
                          </span>
                          <ChevronRight size={16} />
                        </button>
                      ))}
                    </div>
                  </div>
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
                onSendToTutor={(prompt, reply) => {
                  setChat((prev) => [
                    ...prev,
                    { id: `u_${Date.now()}`, role: 'user', text: prompt },
                    { id: `t_${Date.now() + 1}`, role: 'tutor', text: reply, provider: 'Selection AI' }
                  ]);
                  setTab('tutor');
                }}
                onGenerateFlashcards={(matId) => {
                  const mat = STUDY_MATERIALS.find((m) => m.id === matId);
                  if (mat) setSelectedMaterial(mat);
                  generateContent('flashcards', mat?.readingContent);
                }}
                onLaunchQuiz={(t) => {
                  setTopic(t);
                  setTab('tests');
                }}
                videoQuery={videoQuery}
                onChangeVideoQuery={setVideoQuery}
                videos={videos}
                onFindVideos={findVideos}
                isFullScreen={false}
                onToggleFullScreen={() => toggleFullScreen('active')}
              />
            )}

            {tab === 'flashcards' && (
              <FlashcardDeck
                cards={flashcards}
                onRecordReview={handleFlashcardReview}
                onAddCustomCard={handleAddCustomCard}
                onDeleteCard={handleDeleteCard}
                onGenerateFromMaterial={async (matId, count) => {
                  const mat = STUDY_MATERIALS.find((m) => m.id === matId);
                  if (mat) setSelectedMaterial(mat);
                  await generateContent('flashcards', mat?.readingContent);
                  void count;
                }}
                isGenerating={generatingCards}
                isFullScreen={false}
                onToggleFullScreen={() => toggleFullScreen('active')}
              />
            )}

            {tab === 'tests' && renderTestsPanel()}

            {tab === 'tutor' && renderTutorPanel()}

            {tab === 'videos' && (
              <div className="glass rounded-3xl p-5 space-y-4">
                <div className="flex items-center justify-between">
                  <h2 className="text-xl md:text-2xl font-bold">Topic Video Library</h2>
                  <button
                    type="button"
                    onClick={() => toggleFullScreen('active')}
                    className="p-2.5 rounded-xl bg-white/5"
                  >
                    <Maximize2 size={16} />
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
                    onClick={findVideos}
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
                  <h3 className="font-bold text-lg">Google Drive Course Link</h3>
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
                    Open Drive <ExternalLink size={14} />
                  </button>
                  <p className="text-xs text-slate-400 pt-2">
                    Works offline on laptop & mobile once installed as a PWA. Your bundled OM
                    Textbook & Past Papers PDFs are cached automatically in the browser.
                  </p>
                </div>
              </div>
            )}

            {tab === 'past' && (
              <div className="glass rounded-3xl p-5 md:p-6 space-y-4">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <div className="text-xs text-cyan-300 uppercase font-semibold">
                      ICMA ML-1 Past Paper Lab
                    </div>
                    <h2 className="text-xl md:text-2xl font-bold">
                      Past Papers Preparation (Q.1 – Q.10)
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
                    Open Past Papers PDF & Answers
                  </button>
                </div>
                <div className="grid md:grid-cols-3 gap-4">
                  {[
                    ['01', '5-Mark Suggested Solutions', 'Study all 10 official ML-1 past questions inside Read & PDF view.'],
                    ['02', 'Timed OM MCQ Sprint', 'Practice 12+ OM exam MCQs or generate fresh AI MCQs anytime.'],
                    ['03', 'Convert to Flashcards', 'Generate active-recall flip cards straight from the Past Paper pack.']
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
                    disabled={busy}
                    onClick={() => generateContent('typing')}
                    className="px-4 py-3 rounded-xl bg-violet-500/20 hover:bg-violet-500/30 text-xs font-semibold"
                  >
                    Generate New 5-Mark Past Paper Questions
                  </button>
                  <button
                    type="button"
                    onClick={() => generateContent('flashcards', STUDY_MATERIALS[1].readingContent)}
                    className="px-4 py-3 rounded-xl bg-cyan-500/20 hover:bg-cyan-500/30 text-xs font-semibold"
                  >
                    Create Flashcards from Past Papers
                  </button>
                </div>
              </div>
            )}
          </section>
        </div>
      </main>

      {/* Install PWA Instructions Modal (for Safari / browsers without beforeinstallprompt) */}
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
                <strong>Install</strong> icon in the browser address bar (or ⋮ Menu →{' '}
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

      {/* Toast Notification */}
      {toastMsg && (
        <div className="fixed bottom-5 right-5 z-50 glass px-4 py-3 rounded-2xl border-cyan-300/40 text-xs text-cyan-100 shadow-xl">
          {toastMsg}
        </div>
      )}

      <footer className="max-w-7xl mx-auto w-full px-4 pb-6 pt-2 text-xs text-slate-500 flex justify-between">
        <span>ICMAP Smart Tutor PWA • Installable on Laptop & Mobile</span>
        <span>{busy ? 'AI Tutor working…' : 'Ready'}</span>
      </footer>
    </div>
  );
}
