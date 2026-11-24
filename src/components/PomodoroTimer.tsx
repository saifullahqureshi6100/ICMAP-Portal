import React, { useEffect, useState } from 'react';
import {
  Maximize2,
  Minimize2,
  Pause,
  Play,
  RotateCcw,
  Settings2,
  SkipForward
} from 'lucide-react';
import { PomodoroPhase, PomodoroSettings } from '../types';

interface PomodoroTimerProps {
  settings: PomodoroSettings;
  onUpdateSettings: (next: PomodoroSettings) => void;
  onFocusSessionCompleted: (minutesLogged: number) => void;
  isFullScreen: boolean;
  onToggleFullScreen: () => void;
}

export const PomodoroTimer: React.FC<PomodoroTimerProps> = ({
  settings,
  onUpdateSettings,
  onFocusSessionCompleted,
  isFullScreen,
  onToggleFullScreen
}) => {
  const [phase, setPhase] = useState<PomodoroPhase>('work');
  const [remainingSeconds, setRemainingSeconds] = useState(settings.workMinutes * 60);
  const [isRunning, setIsRunning] = useState(false);
  const [completedBlocks, setCompletedBlocks] = useState(0);
  const [showSettings, setShowSettings] = useState(false);

  const [draft, setDraft] = useState<PomodoroSettings>(settings);

  const totalPhaseSeconds =
    phase === 'work'
      ? settings.workMinutes * 60
      : phase === 'short_break'
        ? settings.shortBreakMinutes * 60
        : settings.longBreakMinutes * 60;

  useEffect(() => {
    setDraft(settings);
  }, [settings]);

  useEffect(() => {
    if (!isRunning) return;
    const id = window.setInterval(() => {
      setRemainingSeconds((prev) => {
        if (prev > 1) return prev - 1;
        // Phase completed
        if (phase === 'work') {
          onFocusSessionCompleted(settings.workMinutes);
          const nextBlocks = completedBlocks + 1;
          setCompletedBlocks(nextBlocks);
          const nextPhase: PomodoroPhase =
            nextBlocks % settings.cyclesBeforeLongBreak === 0 ? 'long_break' : 'short_break';
          setPhase(nextPhase);
          if (!settings.autoAdvance) setIsRunning(false);
          return nextPhase === 'long_break'
            ? settings.longBreakMinutes * 60
            : settings.shortBreakMinutes * 60;
        } else {
          setPhase('work');
          if (!settings.autoAdvance) setIsRunning(false);
          return settings.workMinutes * 60;
        }
      });
    }, 1000);
    return () => window.clearInterval(id);
  }, [
    isRunning,
    phase,
    completedBlocks,
    settings,
    onFocusSessionCompleted
  ]);

  function switchPhase(next: PomodoroPhase) {
    setIsRunning(false);
    setPhase(next);
    setRemainingSeconds(
      next === 'work'
        ? settings.workMinutes * 60
        : next === 'short_break'
          ? settings.shortBreakMinutes * 60
          : settings.longBreakMinutes * 60
    );
  }

  function resetPhase() {
    setIsRunning(false);
    setRemainingSeconds(totalPhaseSeconds);
  }

  function skipPhase() {
    if (phase === 'work') {
      const nextPhase: PomodoroPhase =
        (completedBlocks + 1) % settings.cyclesBeforeLongBreak === 0
          ? 'long_break'
          : 'short_break';
      switchPhase(nextPhase);
    } else {
      switchPhase('work');
    }
  }

  function applySettings(e: React.FormEvent) {
    e.preventDefault();
    const cleaned: PomodoroSettings = {
      workMinutes: Math.max(1, Math.min(120, Number(draft.workMinutes) || 25)),
      shortBreakMinutes: Math.max(1, Math.min(60, Number(draft.shortBreakMinutes) || 5)),
      longBreakMinutes: Math.max(5, Math.min(60, Number(draft.longBreakMinutes) || 15)),
      cyclesBeforeLongBreak: Math.max(2, Math.min(8, Number(draft.cyclesBeforeLongBreak) || 4)),
      autoAdvance: draft.autoAdvance
    };
    onUpdateSettings(cleaned);
    setIsRunning(false);
    setRemainingSeconds(
      phase === 'work'
        ? cleaned.workMinutes * 60
        : phase === 'short_break'
          ? cleaned.shortBreakMinutes * 60
          : cleaned.longBreakMinutes * 60
    );
    setShowSettings(false);
  }

  const mins = Math.floor(remainingSeconds / 60);
  const secs = remainingSeconds % 60;
  const timeText = `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  const progress =
    totalPhaseSeconds > 0
      ? Math.max(0, Math.min(1, (totalPhaseSeconds - remainingSeconds) / totalPhaseSeconds))
      : 0;
  const radius = isFullScreen ? 92 : 64;
  const circumference = 2 * Math.PI * radius;
  const strokeDashoffset = circumference * (1 - progress);
  const strokeColor =
    phase === 'work' ? '#22d3ee' : phase === 'short_break' ? '#34d399' : '#a78bfa';

  return (
    <div className={`glass rounded-3xl p-5 ${isFullScreen ? 'max-w-xl mx-auto w-full' : ''}`}>
      <div className="flex items-center justify-between gap-2">
        <div>
          <div className="text-[11px] uppercase tracking-widest text-cyan-300 font-semibold">
            Pomodoro Focus Timer
          </div>
          <div className="text-xs text-slate-400 mt-0.5">
            {completedBlocks} focus blocks done • {settings.workMinutes}m / {settings.shortBreakMinutes}m
          </div>
        </div>
        <div className="flex items-center gap-1.5">
          <button
            type="button"
            onClick={() => setShowSettings((s) => !s)}
            aria-label="Configure Pomodoro intervals"
            className="p-2.5 rounded-xl bg-white/5 hover:bg-white/10 text-slate-200 min-w-[44px] min-h-[44px] flex items-center justify-center"
          >
            <Settings2 size={17} />
          </button>
          <button
            type="button"
            onClick={onToggleFullScreen}
            aria-label={isFullScreen ? 'Exit full-screen timer' : 'Full-screen timer'}
            className="p-2.5 rounded-xl bg-white/5 hover:bg-white/10 text-slate-200 min-w-[44px] min-h-[44px] flex items-center justify-center"
          >
            {isFullScreen ? <Minimize2 size={17} /> : <Maximize2 size={17} />}
          </button>
        </div>
      </div>

      {/* Phase Pills */}
      <div className="flex flex-wrap gap-2 mt-4 justify-center">
        {(
          [
            ['work', 'Focus Work'],
            ['short_break', 'Short Break'],
            ['long_break', 'Long Break']
          ] as [PomodoroPhase, string][]
        ).map(([key, label]) => (
          <button
            key={key}
            type="button"
            onClick={() => switchPhase(key)}
            className={`px-3 py-1.5 rounded-full text-xs font-medium border transition ${
              phase === key
                ? 'bg-cyan-400/20 text-cyan-200 border-cyan-300/40'
                : 'bg-white/5 text-slate-400 border-white/10 hover:text-white'
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {/* Circular Countdown Ring */}
      <div className="flex justify-center my-5">
        <div className="relative inline-flex items-center justify-center">
          <svg
            width={radius * 2 + 24}
            height={radius * 2 + 24}
            className="-rotate-90"
          >
            <circle
              cx={radius + 12}
              cy={radius + 12}
              r={radius}
              stroke="rgba(255,255,255,0.08)"
              strokeWidth={10}
              fill="none"
            />
            <circle
              cx={radius + 12}
              cy={radius + 12}
              r={radius}
              stroke={strokeColor}
              strokeWidth={10}
              strokeLinecap="round"
              strokeDasharray={circumference}
              strokeDashoffset={strokeDashoffset}
              fill="none"
              style={{ transition: 'stroke-dashoffset 450ms ease' }}
            />
          </svg>
          <div className="absolute text-center">
            <div className={`${isFullScreen ? 'text-5xl' : 'text-3xl'} font-extrabold tracking-tight tabular-nums`}>
              {timeText}
            </div>
            <div className="text-[11px] uppercase tracking-widest text-slate-400 mt-1">
              {phase === 'work' ? 'Deep Focus' : phase === 'short_break' ? 'Short Break' : 'Long Break'}
            </div>
          </div>
        </div>
      </div>

      {/* Playback Controls */}
      <div className="flex items-center justify-center gap-2.5">
        <button
          type="button"
          onClick={resetPhase}
          className="px-3.5 py-2.5 rounded-xl bg-white/5 hover:bg-white/10 text-xs flex items-center gap-1.5 min-h-[44px]"
        >
          <RotateCcw size={15} /> Reset
        </button>
        <button
          type="button"
          onClick={() => setIsRunning((r) => !r)}
          className="px-5 py-2.5 rounded-xl bg-gradient-to-r from-cyan-500 to-violet-500 font-semibold text-sm flex items-center gap-2 min-h-[44px]"
        >
          {isRunning ? <Pause size={16} /> : <Play size={16} />}
          {isRunning ? 'Pause' : 'Start Focus'}
        </button>
        <button
          type="button"
          onClick={skipPhase}
          className="px-3.5 py-2.5 rounded-xl bg-white/5 hover:bg-white/10 text-xs flex items-center gap-1.5 min-h-[44px]"
        >
          <SkipForward size={15} /> Skip
        </button>
      </div>

      {/* Interval Configuration Drawer */}
      {showSettings && (
        <form
          onSubmit={applySettings}
          className="mt-5 p-4 rounded-2xl bg-black/30 border border-white/10 space-y-3 text-xs"
        >
          <div className="font-semibold text-cyan-200">Configure Study Intervals</div>
          <div className="grid grid-cols-2 gap-3">
            <label className="space-y-1">
              <span className="text-slate-400 block">Focus (min)</span>
              <input
                type="number"
                min={1}
                max={120}
                value={draft.workMinutes}
                onChange={(e) => setDraft({ ...draft, workMinutes: Number(e.target.value) })}
                className="w-full rounded-lg bg-white/5 border border-white/10 px-2.5 py-2"
              />
            </label>
            <label className="space-y-1">
              <span className="text-slate-400 block">Short Break (min)</span>
              <input
                type="number"
                min={1}
                max={60}
                value={draft.shortBreakMinutes}
                onChange={(e) => setDraft({ ...draft, shortBreakMinutes: Number(e.target.value) })}
                className="w-full rounded-lg bg-white/5 border border-white/10 px-2.5 py-2"
              />
            </label>
            <label className="space-y-1">
              <span className="text-slate-400 block">Long Break (min)</span>
              <input
                type="number"
                min={5}
                max={60}
                value={draft.longBreakMinutes}
                onChange={(e) => setDraft({ ...draft, longBreakMinutes: Number(e.target.value) })}
                className="w-full rounded-lg bg-white/5 border border-white/10 px-2.5 py-2"
              />
            </label>
            <label className="space-y-1">
              <span className="text-slate-400 block">Cycles / Long Break</span>
              <input
                type="number"
                min={2}
                max={8}
                value={draft.cyclesBeforeLongBreak}
                onChange={(e) =>
                  setDraft({ ...draft, cyclesBeforeLongBreak: Number(e.target.value) })
                }
                className="w-full rounded-lg bg-white/5 border border-white/10 px-2.5 py-2"
              />
            </label>
          </div>
          <label className="flex items-center gap-2 pt-1 cursor-pointer">
            <input
              type="checkbox"
              checked={draft.autoAdvance}
              onChange={(e) => setDraft({ ...draft, autoAdvance: e.target.checked })}
            />
            <span className="text-slate-300">Auto-start next focus / break phase</span>
          </label>
          <div className="flex justify-end gap-2 pt-1">
            <button
              type="button"
              onClick={() => setShowSettings(false)}
              className="px-3 py-2 rounded-lg bg-white/5"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-2 rounded-lg bg-cyan-500/25 text-cyan-100 font-semibold"
            >
              Save Intervals
            </button>
          </div>
        </form>
      )}
    </div>
  );
};
