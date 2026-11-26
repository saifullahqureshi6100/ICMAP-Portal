import React, { useState } from 'react';
import {
  AlertTriangle,
  Award,
  BookOpen,
  CalendarClock,
  CheckCircle2,
  Circle,
  Clock,
  Layers,
  Mic,
  PenLine,
  Play,
  RefreshCw,
  Send,
  Sparkles,
  Volume2,
  VolumeX
} from 'lucide-react';
import { DailyBriefPlan, DailyRoutineSlot, TutorChatItem, TutorLang } from '../types';

interface DailyBriefAndCoachProps {
  plan: DailyBriefPlan;
  weakTopics: string[];
  lang: TutorLang;
  onChangeLang: (lang: TutorLang) => void;
  onChangeEnergyMode: (mode: DailyBriefPlan['energyMode']) => void;
  onToggleSlot: (slotId: string) => void;
  onLaunchAction: (actionType: DailyRoutineSlot['actionType'], topicFocus: string) => void;
  onLaunchTodayVideo: (query: string) => void;
  onSpeakText: (text: string, lang: TutorLang) => void;
  onStopSpeech: () => void;
  isSpeaking: boolean;
  onGenerateAiBrief: () => Promise<string>;
  coachMessages: TutorChatItem[];
  onAskCoach: (prompt: string) => Promise<void>;
  onStartVoiceCoach: () => void;
  listeningVoice: boolean;
  coachBusy: boolean;
}

export const DailyBriefAndCoach: React.FC<DailyBriefAndCoachProps> = ({
  plan,
  weakTopics,
  lang,
  onChangeLang,
  onChangeEnergyMode,
  onToggleSlot,
  onLaunchAction,
  onLaunchTodayVideo,
  onSpeakText,
  onStopSpeech,
  isSpeaking,
  onGenerateAiBrief,
  coachMessages,
  onAskCoach,
  onStartVoiceCoach,
  listeningVoice,
  coachBusy
}) => {
  const [showBriefDetails, setShowBriefDetails] = useState(true);
  const [aiBriefText, setAiBriefText] = useState('');
  const [aiBriefLoading, setAiBriefLoading] = useState(false);
  const [coachInput, setCoachInput] = useState('');

  const hoursLabel = (plan.totalStudyMinutes / 60).toFixed(1);
  const completedCount = plan.slots.filter((s) => s.completed).length;

  async function handleBriefButtonClick() {
    setShowBriefDetails(true);
    const briefToRead = aiBriefText || plan.urduSpokenBrief;
    onSpeakText(briefToRead, 'ur');
    if (!aiBriefText && !aiBriefLoading) {
      setAiBriefLoading(true);
      try {
        const fresh = await onGenerateAiBrief();
        if (fresh) setAiBriefText(fresh);
      } catch {
        // Fallback already covers spoken brief offline
      } finally {
        setAiBriefLoading(false);
      }
    }
  }

  async function handleRefreshAiBrief() {
    setAiBriefLoading(true);
    try {
      const fresh = await onGenerateAiBrief();
      if (fresh) {
        setAiBriefText(fresh);
        onSpeakText(fresh, lang);
      }
    } finally {
      setAiBriefLoading(false);
    }
  }

  function submitCoachPrompt(e?: React.FormEvent) {
    e?.preventDefault();
    const q = coachInput.trim();
    if (!q || coachBusy) return;
    setCoachInput('');
    void onAskCoach(q);
  }

  return (
    <div className="grid lg:grid-cols-[1.25fr_1fr] gap-5 items-start">
      {/* LEFT: Brief of the Day + Whole-Day Office Routine */}
      <div className="glass rounded-3xl p-5 md:p-6 space-y-4 border border-cyan-300/25">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-amber-400/15 text-amber-200 text-[11px] font-bold uppercase tracking-wider">
              <Award size={13} /> OM Only • 5 Dec 2026 • {plan.daysToExam} Days Left • Emergency Merit Mode
            </div>
            <h2 className="text-xl md:text-2xl font-extrabold mt-2">{plan.examPhaseTitle}</h2>
            <p className="text-xs text-slate-300 mt-1">
              Today’s Total Target: <b className="text-cyan-200">{hoursLabel} hrs ({plan.totalStudyMinutes} min)</b> •{' '}
              Done: <b className="text-emerald-200">{completedCount}/{plan.slots.length}</b> • Focus:{' '}
              <b className="text-violet-200">{plan.todayTopic}</b>
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <button
              type="button"
              onClick={handleBriefButtonClick}
              className="px-4 py-2.5 rounded-xl bg-gradient-to-r from-cyan-500 to-violet-500 hover:brightness-110 text-xs font-bold flex items-center gap-2 min-h-[44px] shadow-lg shadow-cyan-500/20"
            >
              <Volume2 size={16} /> Brief of the Day (Urdu)
            </button>
            {isSpeaking && (
              <button
                type="button"
                onClick={onStopSpeech}
                className="px-3 py-2.5 rounded-xl bg-rose-500/25 text-rose-100 text-xs font-semibold flex items-center gap-1.5 min-h-[44px]"
              >
                <VolumeX size={15} /> Stop
              </button>
            )}
            <button
              type="button"
              onClick={handleRefreshAiBrief}
              disabled={aiBriefLoading}
              aria-label="Refresh AI Coach Daily Brief"
              className="p-2.5 rounded-xl bg-white/5 hover:bg-white/10 text-xs min-w-[44px] min-h-[44px] flex items-center justify-center"
            >
              <RefreshCw size={15} className={aiBriefLoading ? 'animate-spin' : ''} />
            </button>
          </div>
        </div>

        {/* Advance Mock / Grand Test Alert + Study Technique */}
        <div className="grid sm:grid-cols-2 gap-3">
          <div className="p-3.5 rounded-2xl bg-amber-500/12 border border-amber-300/25 text-xs space-y-1">
            <div className="font-bold text-amber-200 flex items-center gap-1.5">
              <AlertTriangle size={14} /> Upcoming Grand Test / Mock Alert
            </div>
            <p className="text-slate-200 leading-5">{plan.upcomingAlert}</p>
          </div>
          <div className="p-3.5 rounded-2xl bg-cyan-500/12 border border-cyan-300/25 text-xs space-y-1">
            <div className="font-bold text-cyan-200 flex items-center gap-1.5">
              <Sparkles size={14} /> 50-Yr Teacher Study Technique
            </div>
            <p className="text-slate-200 leading-5">{plan.studyTechniqueTip}</p>
          </div>
        </div>

        {/* Energy & Office Meeting Quick Re-Planner */}
        <div className="space-y-1.5">
          <div className="text-[11px] uppercase tracking-wider text-slate-400 font-semibold flex items-center justify-between">
            <span>Adapt Routine to Today’s Office / Recovery State</span>
            <button
              type="button"
              onClick={() => setShowBriefDetails((v) => !v)}
              className="text-cyan-300 hover:underline"
            >
              {showBriefDetails ? 'Hide Timeline' : 'Show Full Timeline'}
            </button>
          </div>
          <div className="flex flex-wrap gap-2">
            {[
              ['normal', 'Normal Office Day'],
              ['busy_office', 'Heavy Office / Late Meeting'],
              ['low_recovery', 'Low Energy / Recovery'],
              ['final_sprint', 'Final Merit Sprint']
            ].map(([key, label]) => (
              <button
                key={key}
                type="button"
                onClick={() => onChangeEnergyMode(key as DailyBriefPlan['energyMode'])}
                className={`px-3 py-1.5 rounded-full text-xs border transition ${
                  plan.energyMode === key
                    ? 'bg-cyan-400/20 text-cyan-100 border-cyan-300/40 font-semibold'
                    : 'bg-white/5 text-slate-400 border-white/10 hover:text-white'
                }`}
              >
                {label}
              </button>
            ))}
          </div>
        </div>

        {/* Spoken / AI Coach Briefing Script */}
        {aiBriefText && (
          <div className="p-3.5 rounded-2xl bg-black/30 border border-violet-300/25 text-xs space-y-2">
            <div className="flex items-center justify-between text-violet-200 font-bold">
              <span>Live Mentor Briefing (Groq / AI Coach)</span>
              <button
                type="button"
                onClick={() => onSpeakText(aiBriefText, lang)}
                className="text-cyan-300 inline-flex items-center gap-1"
              >
                <Volume2 size={13} /> Speak Aloud
              </button>
            </div>
            <div className="whitespace-pre-wrap text-slate-200 leading-5 max-h-44 overflow-y-auto scrollbar">
              {aiBriefText}
            </div>
          </div>
        )}

        {/* Quick Actions for Today's Video + Office Short Test + 5 Cards + Daily Test */}
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => onLaunchTodayVideo(plan.todayVideoQuery)}
            className="px-3 py-2 rounded-xl bg-rose-500/20 hover:bg-rose-500/30 text-rose-100 text-xs font-semibold flex items-center gap-1.5 min-h-[40px]"
          >
            <Play size={13} /> Watch Today’s Topic Video
          </button>
          <button
            type="button"
            onClick={() => onLaunchAction('office_short_test', plan.todayTopic)}
            className="px-3 py-2 rounded-xl bg-amber-500/20 hover:bg-amber-500/30 text-amber-100 text-xs font-semibold flex items-center gap-1.5 min-h-[40px]"
          >
            <Clock size={13} /> Short Test during Office
          </button>
          <button
            type="button"
            onClick={() => onLaunchAction('flashcards', plan.todayTopic)}
            className="px-3 py-2 rounded-xl bg-violet-500/20 hover:bg-violet-500/30 text-violet-100 text-xs font-semibold flex items-center gap-1.5 min-h-[40px]"
          >
            <Layers size={13} /> 5:30 PM • 5 Flashcards
          </button>
          <button
            type="button"
            onClick={() => onLaunchAction('daily_test', plan.todayTopic)}
            className="px-3 py-2 rounded-xl bg-emerald-500/20 hover:bg-emerald-500/30 text-emerald-100 text-xs font-semibold flex items-center gap-1.5 min-h-[40px]"
          >
            <PenLine size={13} /> Test of the Day (60/40)
          </button>
        </div>

        {/* Full Day Office-Worker Time-Blocked Routine */}
        {showBriefDetails && (
          <div className="space-y-2 pt-2 border-t border-white/10">
            <div className="text-xs font-bold text-slate-300 flex items-center gap-1.5">
              <CalendarClock size={14} className="text-cyan-300" /> Full-Day Routine (08:00 AM → 11:30 PM Sleep)
            </div>
            <div className="space-y-2 max-h-[380px] overflow-y-auto scrollbar pr-1">
              {plan.slots.map((slot) => (
                <div
                  key={slot.id}
                  className={`p-3 rounded-2xl border transition flex items-start gap-3 ${
                    slot.completed
                      ? 'bg-emerald-500/10 border-emerald-400/25 opacity-80'
                      : 'bg-white/5 border-white/10'
                  }`}
                >
                  <button
                    type="button"
                    onClick={() => onToggleSlot(slot.id)}
                    aria-label={`Toggle ${slot.label}`}
                    className="mt-0.5 text-cyan-300 shrink-0 min-w-[28px] min-h-[28px] flex items-center justify-center"
                  >
                    {slot.completed ? (
                      <CheckCircle2 size={18} className="text-emerald-300" />
                    ) : (
                      <Circle size={18} />
                    )}
                  </button>
                  <div className="flex-1 min-w-0">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <span className="text-[11px] font-bold text-cyan-200">{slot.timeWindow}</span>
                      {slot.durationMinutes > 0 && (
                        <span className="text-[10px] px-2 py-0.5 rounded-full bg-white/10 text-slate-300">
                          {slot.durationMinutes} min
                        </span>
                      )}
                    </div>
                    <div className="text-xs font-bold mt-0.5">{slot.label}</div>
                    <div className="text-xs text-slate-300 mt-0.5 leading-5">{slot.activity}</div>
                  </div>
                  {slot.actionType !== 'recovery' && (
                    <button
                      type="button"
                      onClick={() => onLaunchAction(slot.actionType, slot.topicFocus)}
                      className="px-2.5 py-1.5 rounded-xl bg-cyan-500/20 hover:bg-cyan-500/30 text-[11px] font-semibold text-cyan-100 shrink-0"
                    >
                      Start
                    </button>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Active Weak Topics Being Targeted */}
        {weakTopics.length > 0 && (
          <div className="pt-2 border-t border-white/10">
            <div className="text-[11px] text-rose-200 font-semibold mb-1.5">
              Weak Topics Auto-Injected into Short Tests & Today’s 5 Cards:
            </div>
            <div className="flex flex-wrap gap-1.5">
              {weakTopics.slice(0, 6).map((w) => (
                <button
                  key={w}
                  type="button"
                  onClick={() => onAskCoach(`Mujhe "${w}" ko asaan Urdu aur corporate finance example se samjhao.`)}
                  className="px-2.5 py-1 rounded-full bg-rose-500/15 border border-rose-300/25 text-rose-100 text-[11px]"
                >
                  {w}
                </button>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* RIGHT: Ask Your 50-Yr Teacher & Strategist (Dashboard Voice + Urdu Chat) */}
      <div className="glass rounded-3xl p-5 flex flex-col min-h-[540px] max-h-[680px] border border-violet-300/25">
        <div className="flex flex-wrap items-center justify-between gap-2 pb-3 border-b border-white/10">
          <div>
            <div className="text-[11px] uppercase tracking-wider text-violet-300 font-bold">
              Ask Personal Teacher (Urdu & Voice)
            </div>
            <h3 className="text-base font-bold">Schedule Change, Stress, or Doubt?</h3>
          </div>
          <div className="flex items-center gap-1.5">
            <button
              type="button"
              onClick={() => onChangeLang(lang === 'ur' ? 'en' : 'ur')}
              className="px-2.5 py-1.5 rounded-lg bg-cyan-500/20 text-cyan-100 text-[11px] font-bold"
            >
              {lang === 'ur' ? 'Urdu Mode ON' : 'English'}
            </button>
          </div>
        </div>

        {/* One-Tap Teacher Situation Prompts */}
        <div className="flex gap-1.5 overflow-x-auto py-2.5 scrollbar shrink-0">
          {[
            'Office meeting aa gayi hai, aaj ka plan adjust karein',
            'Bahut thakan aur exam tension ho rahi hai',
            'Balanced Scorecard vs Building Block Urdu mein samjhao',
            'Mintzberg 5 parts office example se clear karein'
          ].map((preset) => (
            <button
              key={preset}
              type="button"
              onClick={() => onAskCoach(preset)}
              className="px-2.5 py-1.5 rounded-xl bg-white/5 hover:bg-white/10 text-[11px] whitespace-nowrap border border-white/10"
            >
              {preset}
            </button>
          ))}
        </div>

        <div className="flex-1 overflow-y-auto scrollbar space-y-2.5 pr-1">
          {coachMessages.slice(-8).map((msg) => (
            <div
              key={msg.id}
              className={`p-3 rounded-2xl text-xs leading-5 ${
                msg.role === 'user'
                  ? 'ml-6 bg-cyan-500/18 text-cyan-50'
                  : 'mr-3 bg-white/5 text-slate-200 border border-white/10'
              }`}
            >
              <div className="flex items-center justify-between text-[10px] text-slate-400 mb-1">
                <span>{msg.role === 'user' ? 'You' : 'Ustad & Strategist'}</span>
                {msg.role === 'tutor' && (
                  <button
                    type="button"
                    onClick={() => onSpeakText(msg.text, lang)}
                    className="text-cyan-300 inline-flex items-center gap-1"
                  >
                    <Volume2 size={11} /> Suniye
                  </button>
                )}
              </div>
              <div className="whitespace-pre-wrap">{msg.text}</div>
            </div>
          ))}
          {coachBusy && (
            <div className="p-3 rounded-2xl bg-white/5 text-xs text-cyan-200">
              Ustad soch kar Urdu mein jawab tayar kar rahe hain…
            </div>
          )}
        </div>

        <form onSubmit={submitCoachPrompt} className="flex gap-2 mt-3 pt-2 border-t border-white/10">
          <button
            type="button"
            disabled={coachBusy}
            onClick={onStartVoiceCoach}
            aria-label="Ask teacher in Urdu voice"
            className={`px-3 rounded-xl min-w-[44px] min-h-[44px] flex items-center justify-center ${
              listeningVoice
                ? 'bg-rose-500/35 text-rose-100 animate-pulse'
                : 'bg-violet-500/25 hover:bg-violet-500/35 text-violet-100'
            }`}
          >
            <Mic size={16} />
          </button>
          <input
            value={coachInput}
            onChange={(e) => setCoachInput(e.target.value)}
            placeholder={
              listeningVoice
                ? 'Boliyeh — main sun raha hoon...'
                : 'Meeting, fatigue, ya topic Urdu mein poochein...'
            }
            className="flex-1 bg-black/30 border border-white/10 rounded-xl px-3 py-2 text-xs"
          />
          <button
            type="submit"
            disabled={coachBusy || !coachInput.trim()}
            aria-label="Send message to teacher"
            className="px-3.5 rounded-xl bg-cyan-500/25 hover:bg-cyan-500/35 min-w-[44px] min-h-[44px] flex items-center justify-center"
          >
            <Send size={15} />
          </button>
        </form>
      </div>
    </div>
  );
};
