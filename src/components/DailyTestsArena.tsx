import React, { useMemo, useState } from 'react';
import {
  Award,
  CheckCircle2,
  Clock,
  Maximize2,
  Minimize2,
  PenLine,
  Sparkles,
  Volume2
} from 'lucide-react';
import { buildTestPaper } from '../data/strategistEngine';
import { mapTopicToPortionCode } from '../data/portionAnalytics';
import {
  McqQuestionItem,
  OmPortionCode,
  TestAttemptSummary,
  TestModeKind,
  TutorLang,
  TypingQuestionItem
} from '../types';

interface DailyTestsArenaProps {
  weakTopics: string[];
  recentAttempts: TestAttemptSummary[];
  initialMode?: TestModeKind;
  initialPortionFilter?: string;
  lang: TutorLang;
  isFullScreen: boolean;
  onToggleFullScreen: () => void;
  onGenerateAiTest: (action: 'mcq' | 'numerical' | 'typing', topic: string, count: number) => Promise<{
    mcqs: McqQuestionItem[];
    typing: TypingQuestionItem[];
  }>;
  onGradeTypedAnswer: (question: TypingQuestionItem, userAnswer: string) => Promise<string>;
  onSpeakText: (text: string, lang: TutorLang) => void;
  onTestCompleted: (summary: TestAttemptSummary) => void;
}

export const DailyTestsArena: React.FC<DailyTestsArenaProps> = ({
  weakTopics,
  recentAttempts,
  initialMode = 'daily_test',
  initialPortionFilter = 'part_a_ops',
  lang,
  isFullScreen,
  onToggleFullScreen,
  onGenerateAiTest,
  onGradeTypedAnswer,
  onSpeakText,
  onTestCompleted
}) => {
  const [mode, setMode] = useState<TestModeKind>(initialMode);
  const [portionFilter, setPortionFilter] = useState<string>(initialPortionFilter);
  const [paperTitle, setPaperTitle] = useState<string>(() =>
    buildTestPaper(initialMode, weakTopics, initialPortionFilter).title
  );
  const [mcqs, setMcqs] = useState<McqQuestionItem[]>(() =>
    buildTestPaper(initialMode, weakTopics, initialPortionFilter).mcqs
  );
  const [typingQs, setTypingQs] = useState<TypingQuestionItem[]>(() =>
    buildTestPaper(initialMode, weakTopics, initialPortionFilter).typing
  );

  const [selectedAnswers, setSelectedAnswers] = useState<Record<number, number>>({});
  const [typedDrafts, setTypedDrafts] = useState<Record<string, string>>({});
  const [gradingFeedback, setGradingFeedback] = useState<Record<string, string>>({});
  const [gradingId, setGradingId] = useState<string>('');
  const [submitted, setSubmitted] = useState(false);
  const [activeSection, setActiveSection] = useState<'mcqs' | 'typing'>('mcqs');
  const [aiBusy, setAiBusy] = useState(false);
  const [aiTopic, setAiTopic] = useState(weakTopics[0] || 'Balanced Scorecard & Mintzberg');

  function loadPreset(nextMode: TestModeKind, customPortion?: string) {
    const built = buildTestPaper(nextMode, weakTopics, customPortion ?? portionFilter);
    setMode(nextMode);
    if (customPortion) setPortionFilter(customPortion);
    setPaperTitle(built.title);
    setMcqs(built.mcqs);
    setTypingQs(built.typing);
    setSelectedAnswers({});
    setTypedDrafts({});
    setGradingFeedback({});
    setSubmitted(false);
    setActiveSection('mcqs');
  }

  async function handleGenerateAiSet(kind: 'mcq' | 'numerical' | 'typing') {
    setAiBusy(true);
    try {
      const res = await onGenerateAiTest(kind, aiTopic, kind === 'typing' ? 4 : 10);
      if (kind === 'typing' && res.typing.length > 0) {
        setTypingQs(res.typing);
        setActiveSection('typing');
        setPaperTitle(`AI 5-Mark ICMA / CIMA / ACCA Typing Drill (${aiTopic})`);
      } else if (res.mcqs.length > 0) {
        setMcqs(res.mcqs);
        setSelectedAnswers({});
        setSubmitted(false);
        setActiveSection('mcqs');
        setPaperTitle(`AI ${kind.toUpperCase()} Challenge (${aiTopic})`);
      }
    } finally {
      setAiBusy(false);
    }
  }

  async function handleGradeTyping(q: TypingQuestionItem) {
    const answer = (typedDrafts[q.id] || '').trim();
    if (!answer) return;
    setGradingId(q.id);
    try {
      const reply = await onGradeTypedAnswer(q, answer);
      setGradingFeedback((prev) => ({ ...prev, [q.id]: reply }));
    } finally {
      setGradingId('');
    }
  }

  const mcqCorrect = useMemo(
    () => mcqs.filter((q, idx) => selectedAnswers[idx] === q.answer).length,
    [mcqs, selectedAnswers]
  );

  const typingCompletedCount = useMemo(
    () => typingQs.filter((q) => (typedDrafts[q.id] || '').trim().length >= 25).length,
    [typingQs, typedDrafts]
  );

  function submitTestAndDetectWeak() {
    setSubmitted(true);
    // Calculate accuracy per topicTag (< 70% triggers adaptive plan update) and per Syllabus Portion
    const stats = new Map<string, { right: number; total: number }>();
    const portionScores: Partial<Record<OmPortionCode, { right: number; total: number }>> = {};

    mcqs.forEach((q, idx) => {
      const isCorrect = selectedAnswers[idx] === q.answer;
      const tag = q.topicTag || 'General OM';
      const cur = stats.get(tag) || { right: 0, total: 0 };
      cur.total += 1;
      if (isCorrect) cur.right += 1;
      stats.set(tag, cur);

      const pCode = mapTopicToPortionCode(q.bankId || q.topicTag || '');
      const pCur = portionScores[pCode] || { right: 0, total: 0 };
      pCur.total += 1;
      if (isCorrect) pCur.right += 1;
      portionScores[pCode] = pCur;
    });

    const detectedWeak: string[] = [];
    stats.forEach((val, key) => {
      if (val.total > 0 && val.right / val.total < 0.7) {
        detectedWeak.push(key);
      }
    });

    onTestCompleted({
      id: `attempt_${Date.now()}`,
      kind: mode,
      title: paperTitle,
      mcqCorrect,
      mcqTotal: mcqs.length,
      typingCompleted: typingCompletedCount,
      typingTotal: typingQs.length,
      weakTopicsDetected: detectedWeak,
      portionScores,
      timestamp: Date.now()
    });
  }

  const presets: Array<[TestModeKind, string, string]> = [
    ['office_short', 'Short Test during Office', '10 Quick MCQs (Lunch Break)'],
    ['daily_test', 'Test of the Day (60/40)', '22 MCQs + 4 Typing Qs'],
    ['weekly_grand', 'Weekly Grand Test', '25 MCQs + 4 Typing Qs'],
    ['portion_grand', 'Portion Grand Test', 'Part A / B / C / D'],
    ['half_book_mock', 'Half-Book Mock', 'Units 1–9 or 10–14'],
    ['full_course_mock', 'Final Full-Course Mock', '30 MCQs + 5 Typing Qs']
  ];

  return (
    <div className="space-y-5">
      {/* Header & Presets */}
      <div className="glass rounded-3xl p-5 space-y-4">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <div className="text-xs uppercase tracking-widest text-cyan-300 font-semibold flex items-center gap-1.5">
              <Award size={14} /> ICMA Pattern: 60% MCQs • 40% Typing (Past Papers + ACCA/CIMA/ICAEW)
            </div>
            <h2 className="text-xl md:text-2xl font-bold mt-1">{paperTitle}</h2>
            <p className="text-xs text-slate-400 mt-1">
              Submitting updates your Daily Plan, adds weak topics (&lt;70%) to tomorrow’s Short Test during Office, and feeds Today’s 5 Flashcards.
            </p>
          </div>

          <button
            type="button"
            onClick={onToggleFullScreen}
            aria-label="Toggle full-screen test arena"
            className="px-3.5 py-2.5 rounded-xl bg-white/5 hover:bg-white/10 text-xs font-semibold flex items-center gap-1.5 min-h-[44px]"
          >
            {isFullScreen ? <Minimize2 size={15} /> : <Maximize2 size={15} />}
            {isFullScreen ? 'Exit Full Screen' : 'Full-Screen Exam Mode'}
          </button>
        </div>

        {/* Preset Test Mode Buttons */}
        <div className="grid grid-cols-2 md:grid-cols-3 gap-2.5">
          {presets.map(([key, title, sub]) => (
            <button
              key={key}
              type="button"
              onClick={() => loadPreset(key)}
              className={`p-3 rounded-2xl text-left border transition ${
                mode === key
                  ? 'bg-cyan-500/20 border-cyan-300/45 text-white'
                  : 'bg-white/5 border-white/10 text-slate-300 hover:bg-white/10'
              }`}
            >
              <div className="text-xs font-bold">{title}</div>
              <div className="text-[11px] text-slate-400 mt-0.5">{sub}</div>
            </button>
          ))}
        </div>

        {/* Sub-filter for Portion Grand or Half-Book Mock */}
        {(mode === 'portion_grand' || mode === 'half_book_mock') && (
          <div className="flex flex-wrap gap-2 pt-1">
            {mode === 'portion_grand'
              ? [
                  ['part_a_ops', 'Part A: Operations (Units 1–7)'],
                  ['part_b_hrm', 'Part B: HRM (Units 8–9)'],
                  ['part_c_mkt', 'Part C: Marketing (Units 10–12)'],
                  ['part_d_epm', 'Part D: Performance Mgmt (Units 13–14)']
                ].map(([code, lbl]) => (
                  <button
                    key={code}
                    type="button"
                    onClick={() => loadPreset('portion_grand', code)}
                    className={`px-3 py-1.5 rounded-full text-xs border ${
                      portionFilter === code
                        ? 'bg-violet-500/25 border-violet-300/40 text-violet-100'
                        : 'bg-white/5 border-white/10 text-slate-400'
                    }`}
                  >
                    {lbl}
                  </button>
                ))
              : [
                  ['first_half', '1st Half Book (Units 1–9)'],
                  ['second_half', '2nd Half Book (Units 10–14)']
                ].map(([code, lbl]) => (
                  <button
                    key={code}
                    type="button"
                    onClick={() => loadPreset('half_book_mock', code)}
                    className={`px-3 py-1.5 rounded-full text-xs border ${
                      portionFilter === code
                        ? 'bg-violet-500/25 border-violet-300/40 text-violet-100'
                        : 'bg-white/5 border-white/10 text-slate-400'
                    }`}
                  >
                    {lbl}
                  </button>
                ))}
          </div>
        )}

        {/* AI Custom Test Generator Row */}
        <div className="pt-3 border-t border-white/10 flex flex-wrap gap-2 items-center">
          <input
            value={aiTopic}
            onChange={(e) => setAiTopic(e.target.value)}
            placeholder="Target topic for AI MCQs / Typing Qs..."
            className="flex-1 min-w-[200px] bg-black/30 border border-white/10 rounded-xl px-3 py-2 text-xs"
          />
          <button
            type="button"
            disabled={aiBusy}
            onClick={() => handleGenerateAiSet('mcq')}
            className="px-3 py-2 rounded-xl bg-cyan-500/20 text-cyan-100 text-xs font-semibold flex items-center gap-1 min-h-[40px]"
          >
            <Sparkles size={13} /> {aiBusy ? 'Generating…' : 'AI 10 MCQs (ACCA/CIMA/ICMA)'}
          </button>
          <button
            type="button"
            disabled={aiBusy}
            onClick={() => handleGenerateAiSet('typing')}
            className="px-3 py-2 rounded-xl bg-violet-500/20 text-violet-100 text-xs font-semibold flex items-center gap-1 min-h-[40px]"
          >
            <PenLine size={13} /> AI 4 Typing Qs
          </button>
        </div>
      </div>

      {/* Section Switcher: Part 1 MCQs (60%) vs Part 2 Typing (40%) */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex gap-2">
          <button
            type="button"
            onClick={() => setActiveSection('mcqs')}
            className={`px-4 py-2.5 rounded-xl text-xs font-bold min-h-[44px] ${
              activeSection === 'mcqs'
                ? 'bg-cyan-500/25 text-cyan-100 border border-cyan-300/40'
                : 'glass text-slate-300'
            }`}
          >
            Section A • MCQs (60% Weight) — {Object.keys(selectedAnswers).length}/{mcqs.length} Answered
          </button>
          {typingQs.length > 0 && (
            <button
              type="button"
              onClick={() => setActiveSection('typing')}
              className={`px-4 py-2.5 rounded-xl text-xs font-bold min-h-[44px] ${
                activeSection === 'typing'
                  ? 'bg-violet-500/25 text-violet-100 border border-violet-300/40'
                  : 'glass text-slate-300'
              }`}
            >
              Section B • Typing 5-Mark Qs (40% Weight) — {typingCompletedCount}/{typingQs.length} Drafted
            </button>
          )}
        </div>

        <button
          type="button"
          onClick={submitTestAndDetectWeak}
          className="px-5 py-2.5 rounded-xl bg-gradient-to-r from-emerald-500 to-cyan-500 text-xs font-bold text-slate-950 min-h-[44px]"
        >
          Submit Paper & Update Plan
        </button>
      </div>

      {/* Submitted Result Summary */}
      {submitted && (
        <div className="glass rounded-3xl p-5 border border-emerald-400/35 space-y-2">
          <div className="flex items-center gap-2 text-emerald-200 font-bold text-base">
            <CheckCircle2 size={20} /> Score Recorded: {mcqCorrect}/{mcqs.length} MCQs (
            {mcqs.length ? Math.round((mcqCorrect / mcqs.length) * 100) : 0}%) • Typing Attempted:{' '}
            {typingCompletedCount}/{typingQs.length}
          </div>
          <p className="text-xs text-slate-300">
            Topics below 70% have been automatically prioritized into tomorrow’s Short Test during Office and Today’s 5 Flashcards.
          </p>
        </div>
      )}

      {/* Active Section Rendering */}
      {activeSection === 'mcqs' ? (
        <div className="space-y-4">
          {mcqs.map((q, qIdx) => {
            const picked = selectedAnswers[qIdx];
            return (
              <div key={q.id} className="glass rounded-3xl p-5 space-y-3">
                <div className="flex flex-wrap items-center justify-between gap-2 text-[11px]">
                  <span className="px-2.5 py-0.5 rounded-full bg-cyan-500/15 text-cyan-200 font-semibold">
                    Q{qIdx + 1} • {q.topicTag}
                  </span>
                  {q.sourceBenchmark && (
                    <span className="text-slate-400">{q.sourceBenchmark}</span>
                  )}
                </div>
                <div className="text-sm md:text-base font-semibold leading-6">{q.question}</div>
                <div className="grid sm:grid-cols-2 gap-2.5">
                  {q.options.map((opt, oIdx) => {
                    const isPicked = picked === oIdx;
                    const showCorrect = submitted && oIdx === q.answer;
                    const showWrong = submitted && isPicked && oIdx !== q.answer;
                    return (
                      <button
                        key={oIdx}
                        type="button"
                        disabled={submitted}
                        onClick={() =>
                          setSelectedAnswers((prev) => ({ ...prev, [qIdx]: oIdx }))
                        }
                        className={`text-left p-3.5 rounded-xl border text-xs md:text-sm transition min-h-[48px] ${
                          showCorrect
                            ? 'bg-emerald-500/20 border-emerald-400 text-emerald-100'
                            : showWrong
                              ? 'bg-rose-500/20 border-rose-400 text-rose-100'
                              : isPicked
                                ? 'bg-cyan-500/20 border-cyan-300 text-white'
                                : 'bg-white/5 border-white/10 hover:bg-white/10 text-slate-200'
                        }`}
                      >
                        <b>{String.fromCharCode(65 + oIdx)}.</b> {opt}
                      </button>
                    );
                  })}
                </div>
                {submitted && (
                  <div className="p-3 rounded-xl bg-black/30 text-xs text-slate-300 leading-5">
                    <b className="text-emerald-300">Examiner Explanation:</b> {q.explanation}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      ) : (
        <div className="space-y-4">
          {typingQs.map((q, idx) => {
            const draft = typedDrafts[q.id] || '';
            const feedback = gradingFeedback[q.id] || '';
            const matchedKeywords = q.keywords.filter((kw) =>
              draft.toLowerCase().includes(kw.toLowerCase())
            );
            return (
              <div key={q.id} className="glass rounded-3xl p-5 space-y-3">
                <div className="flex flex-wrap items-center justify-between gap-2 text-[11px]">
                  <span className="px-2.5 py-0.5 rounded-full bg-violet-500/20 text-violet-200 font-semibold">
                    Typing Q{idx + 1} ({q.marks} Marks) • {q.topicTag}
                  </span>
                  <span className="text-slate-400">{q.sourceBenchmark}</span>
                </div>
                <div className="text-sm md:text-base font-bold leading-6">{q.question}</div>

                <textarea
                  value={draft}
                  onChange={(e) =>
                    setTypedDrafts((prev) => ({ ...prev, [q.id]: e.target.value }))
                  }
                  rows={5}
                  placeholder="Type your bulleted ICMA Merit answer here (use clear headings & numbered points)..."
                  className="w-full rounded-2xl bg-black/30 border border-white/15 p-3.5 text-xs md:text-sm leading-6"
                />

                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div className="text-[11px] text-slate-400">
                    Keywords hit: <b className="text-cyan-200">{matchedKeywords.length}/{q.keywords.length}</b>
                  </div>
                  <button
                    type="button"
                    disabled={gradingId === q.id || !draft.trim()}
                    onClick={() => handleGradeTyping(q)}
                    className="px-4 py-2 rounded-xl bg-violet-500/25 hover:bg-violet-500/35 text-violet-100 text-xs font-bold flex items-center gap-1.5 min-h-[40px]"
                  >
                    <Sparkles size={14} />
                    {gradingId === q.id ? 'Checking like Examiner...' : 'AI Examiner Grade (Urdu/Eng)'}
                  </button>
                </div>

                {feedback && (
                  <div className="p-3.5 rounded-2xl bg-cyan-500/10 border border-cyan-300/25 text-xs space-y-2">
                    <div className="flex items-center justify-between text-cyan-200 font-bold">
                      <span>Examiner Feedback</span>
                      <button
                        type="button"
                        onClick={() => onSpeakText(feedback, lang)}
                        className="inline-flex items-center gap-1 text-cyan-300"
                      >
                        <Volume2 size={13} /> Listen
                      </button>
                    </div>
                    <div className="whitespace-pre-wrap text-slate-200 leading-5">{feedback}</div>
                  </div>
                )}

                {submitted && (
                  <div className="p-3.5 rounded-2xl bg-emerald-500/10 border border-emerald-400/25 text-xs space-y-1">
                    <div className="font-bold text-emerald-200">Official Model Answer:</div>
                    <div className="whitespace-pre-wrap text-slate-200 leading-5">{q.modelAnswer}</div>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* Recent Attempt History */}
      {recentAttempts.length > 0 && (
        <div className="glass rounded-3xl p-5 space-y-2">
          <div className="text-xs uppercase tracking-wider text-slate-400 font-semibold flex items-center gap-1.5">
            <Clock size={13} /> Recent Daily Test & Mock Scores
          </div>
          <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-2.5">
            {recentAttempts.slice(0, 6).map((a) => (
              <div key={a.id} className="p-3 rounded-2xl bg-white/5 border border-white/10 text-xs">
                <div className="font-bold truncate">{a.title}</div>
                <div className="text-cyan-200 mt-1">
                  MCQs: {a.mcqCorrect}/{a.mcqTotal} • Typing: {a.typingCompleted}/{a.typingTotal}
                </div>
                {a.weakTopicsDetected.length > 0 && (
                  <div className="text-[11px] text-rose-200 mt-1 truncate">
                    Needs focus: {a.weakTopicsDetected.join(', ')}
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
