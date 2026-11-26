import React from 'react';
import {
  Award,
  BookOpen,
  CheckCircle2,
  Clock,
  Flame,
  Layers,
  Play,
  Plus,
  Sparkles,
  Target,
  TrendingUp
} from 'lucide-react';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from 'recharts';
import { OmPortionCode, OmPortionStat } from '../types';

interface ExecutiveDashboardProps {
  portions: OmPortionStat[];
  daysToExam: number;
  weakTopics: string[];
  studyMinutesTotal: number;
  dailyStudyMinutes: Record<string, number>;
  completedMilestonesCount: number;
  totalMilestonesCount: number;
  testAttemptsCount: number;
  onLogStudyMinutes: (minutes: number, sourceLabel?: string) => void;
  onPracticePortion: (
    bankFilter: 'part_a_ops' | 'part_b_hrm' | 'part_c_mkt' | 'part_d_epm'
  ) => void;
  onStudyPortion: (portion: OmPortionStat) => void;
  onOpenRepairTopic: (topic: string) => void;
}

const ACCENT_MAP: Record<
  OmPortionCode,
  {
    border: string;
    badgeBg: string;
    barBg: string;
    text: string;
    stripBg: string;
  }
> = {
  'OM-A': {
    border: 'border-cyan-400/35',
    badgeBg: 'bg-cyan-500/20 text-cyan-200 border-cyan-400/30',
    barBg: 'bg-gradient-to-r from-cyan-400 to-sky-400',
    text: 'text-cyan-300',
    stripBg: 'bg-cyan-400'
  },
  'OM-B': {
    border: 'border-violet-400/30',
    badgeBg: 'bg-violet-500/20 text-violet-200 border-violet-400/30',
    barBg: 'bg-gradient-to-r from-violet-400 to-fuchsia-400',
    text: 'text-violet-300',
    stripBg: 'bg-violet-400'
  },
  'OM-C': {
    border: 'border-amber-400/35',
    badgeBg: 'bg-amber-500/20 text-amber-200 border-amber-400/30',
    barBg: 'bg-gradient-to-r from-amber-400 to-orange-400',
    text: 'text-amber-300',
    stripBg: 'bg-amber-400'
  },
  'OM-D': {
    border: 'border-emerald-400/30',
    badgeBg: 'bg-emerald-500/20 text-emerald-200 border-emerald-400/30',
    barBg: 'bg-gradient-to-r from-emerald-400 to-teal-400',
    text: 'text-emerald-300',
    stripBg: 'bg-emerald-400'
  }
};

function readinessLabel(pct: number): { label: string; cls: string } {
  if (pct >= 85) {
    return {
      label: 'Merit Zone (85%+)',
      cls: 'bg-emerald-500/20 text-emerald-200 border-emerald-400/35'
    };
  }
  if (pct >= 65) {
    return {
      label: 'Exam Ready',
      cls: 'bg-cyan-500/20 text-cyan-200 border-cyan-400/35'
    };
  }
  if (pct >= 30) {
    return {
      label: 'In Progress',
      cls: 'bg-amber-500/20 text-amber-200 border-amber-400/35'
    };
  }
  return {
    label: 'Priority Launch',
    cls: 'bg-rose-500/20 text-rose-200 border-rose-400/35'
  };
}

function toLocalIsoDate(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

interface WeeklyStudyPoint {
  dateKey: string;
  dayShort: string;
  dateLabel: string;
  minutes: number;
  isToday: boolean;
}

function buildCurrentWeekStudySeries(
  dailyStudyMinutes: Record<string, number>,
  referenceDate = new Date()
): WeeklyStudyPoint[] {
  const todayKey = toLocalIsoDate(referenceDate);
  const start = new Date(
    referenceDate.getFullYear(),
    referenceDate.getMonth(),
    referenceDate.getDate()
  );
  // Align week start to Monday (Mon .. Sun)
  const dayOfWeek = start.getDay();
  const diffToMonday = dayOfWeek === 0 ? -6 : 1 - dayOfWeek;
  start.setDate(start.getDate() + diffToMonday);

  const dayNames = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
  const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

  return dayNames.map((dayShort, idx) => {
    const d = new Date(start.getFullYear(), start.getMonth(), start.getDate() + idx);
    const key = toLocalIsoDate(d);
    return {
      dateKey: key,
      dayShort,
      dateLabel: `${dayShort} ${d.getDate()} ${monthNames[d.getMonth()]}`,
      minutes: Math.max(0, Math.round(dailyStudyMinutes[key] || 0)),
      isToday: key === todayKey
    };
  });
}

export const ExecutiveDashboard: React.FC<ExecutiveDashboardProps> = ({
  portions,
  daysToExam,
  weakTopics,
  studyMinutesTotal,
  dailyStudyMinutes,
  completedMilestonesCount,
  totalMilestonesCount,
  testAttemptsCount,
  onLogStudyMinutes,
  onPracticePortion,
  onStudyPortion,
  onOpenRepairTopic
}) => {
  const projectedTotalScore = Number(
    portions.reduce((acc, p) => acc + p.projectedMarks, 0).toFixed(1)
  );
  const partA = portions.find((p) => p.code === 'OM-A');
  const partC = portions.find((p) => p.code === 'OM-C');
  const highYieldMarks = Number(
    ((partA?.projectedMarks || 0) + (partC?.projectedMarks || 0)).toFixed(1)
  );
  const highYieldPct = Math.round((highYieldMarks / 60) * 100);

  const testedPortions = portions.filter((p) => p.testMarksPct !== null);
  const overallTestAvg = testedPortions.length
    ? Math.round(
        testedPortions.reduce((sum, p) => sum + (p.testMarksPct || 0), 0) /
          testedPortions.length
      )
    : null;

  const avgConceptPct = Math.round(
    portions.reduce((sum, p) => sum + p.conceptsPct, 0) / Math.max(1, portions.length)
  );

  const chartData = portions.map((p) => ({
    name: `${p.partLabel} (${p.weightPercent}%)`,
    Weight: p.weightPercent,
    Preparation: p.overallPrepPct,
    TestMarks: p.testMarksPct ?? 0
  }));

  const weeklySeries = buildCurrentWeekStudySeries(dailyStudyMinutes);
  const weeklyMinutesTotal = weeklySeries.reduce((sum, d) => sum + d.minutes, 0);
  const activeWeekDays = weeklySeries.filter((d) => d.minutes > 0).length;
  const todayEntry = weeklySeries.find((d) => d.isToday);
  const elapsedWeekDays = Math.max(1, weeklySeries.findIndex((d) => d.isToday) + 1);
  const weeklyDailyAverage = Math.round(weeklyMinutesTotal / elapsedWeekDays);
  const weekRangeLabel = `${weeklySeries[0]?.dateLabel || ''} – ${weeklySeries[6]?.dateLabel || ''}`;

  return (
    <div className="space-y-5">
      {/* 1. Executive Merit Command Banner */}
      <div className="glass rounded-3xl p-5 md:p-6 border border-cyan-400/25 bg-gradient-to-br from-slate-900/95 via-cyan-950/35 to-violet-950/35 shadow-xl shadow-cyan-950/20">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="space-y-1.5">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-amber-400/15 border border-amber-300/35 text-amber-200 text-[11px] font-bold uppercase tracking-wider">
              <Award size={13} /> ICMA Pakistan Study Scheme 2025 • M3 Organizational Management (ML-1)
            </div>
            <h1 className="text-2xl md:text-3xl font-extrabold tracking-tight">
              Executive Merit & Syllabus Weightage Command Center
            </h1>
            <p className="text-xs md:text-sm text-slate-300 max-w-2xl">
              Live preparation till now weighted by official ICMA Final Exam marks (
              <b className="text-cyan-200">Part A 35% + Part C 25% = 60% High-Yield Core</b>
              ), concept mastery, plan execution & 60/40 test scores.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <div className="rounded-2xl bg-black/35 border border-cyan-300/25 px-4 py-3 text-right">
              <div className="text-[10px] uppercase tracking-wider text-slate-400">
                Projected Final Exam Score
              </div>
              <div className="text-3xl font-black text-cyan-200 leading-tight">
                {projectedTotalScore}
                <span className="text-sm font-semibold text-slate-400"> / 100</span>
              </div>
              <div className="text-[11px] text-emerald-300 font-semibold">
                Merit Goal: 85.0+ • {daysToExam}d to 5 Dec
              </div>
            </div>
          </div>
        </div>

        {/* 4 Executive Telemetry Tiles */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 mt-5">
          <div className="rounded-2xl bg-white/5 border border-white/10 p-3.5">
            <div className="flex items-center justify-between text-xs text-slate-400">
              <span>60% High-Yield Core</span>
              <Flame size={15} className="text-amber-300" />
            </div>
            <div className="text-xl font-bold mt-1 text-amber-200">
              {highYieldMarks} / 60 <span className="text-xs font-medium text-slate-400">({highYieldPct}%)</span>
            </div>
            <div className="text-[11px] text-slate-400 mt-0.5">Part A (35%) + Part C (25%)</div>
          </div>

          <div className="rounded-2xl bg-white/5 border border-white/10 p-3.5">
            <div className="flex items-center justify-between text-xs text-slate-400">
              <span>Concepts & Notes</span>
              <BookOpen size={15} className="text-cyan-300" />
            </div>
            <div className="text-xl font-bold mt-1 text-cyan-200">{avgConceptPct}%</div>
            <div className="text-[11px] text-slate-400 mt-0.5">
              {studyMinutesTotal}m deep focus logged
            </div>
          </div>

          <div className="rounded-2xl bg-white/5 border border-white/10 p-3.5">
            <div className="flex items-center justify-between text-xs text-slate-400">
              <span>Test Marks Average</span>
              <Target size={15} className="text-emerald-300" />
            </div>
            <div className="text-xl font-bold mt-1 text-emerald-200">
              {overallTestAvg !== null ? `${overallTestAvg}%` : '--'}
            </div>
            <div className="text-[11px] text-slate-400 mt-0.5">
              {testAttemptsCount} attempt{testAttemptsCount === 1 ? '' : 's'} graded
            </div>
          </div>

          <div className="rounded-2xl bg-white/5 border border-white/10 p-3.5">
            <div className="flex items-center justify-between text-xs text-slate-400">
              <span>Plan Milestones</span>
              <CheckCircle2 size={15} className="text-violet-300" />
            </div>
            <div className="text-xl font-bold mt-1 text-violet-200">
              {completedMilestonesCount}/{totalMilestonesCount}
            </div>
            <div className="text-[11px] text-slate-400 mt-0.5">
              60% MCQ • 40% Typing Pattern
            </div>
          </div>
        </div>

        {/* Official Final Exam Weightage Allocation Bar (35% | 25% | 20% | 20%) */}
        <div className="mt-5 rounded-2xl bg-black/30 border border-white/10 p-3.5 space-y-2">
          <div className="flex flex-wrap items-center justify-between gap-2 text-xs">
            <span className="font-semibold text-slate-200 flex items-center gap-1.5">
              <Layers size={14} className="text-cyan-300" /> Final Exam Weightage Distribution (100 Marks)
            </span>
            <span className="text-[11px] text-slate-400">
              Part A 35% • Part C 25% • Part B 20% • Part D 20%
            </span>
          </div>
          <div className="w-full h-4 rounded-full overflow-hidden bg-slate-800/80 flex gap-0.5 p-0.5">
            {portions.map((p) => {
              const style = ACCENT_MAP[p.code];
              return (
                <div
                  key={p.code}
                  style={{ width: `${p.weightPercent}%` }}
                  title={`${p.partLabel} (${p.weightPercent}%): ${p.overallPrepPct}% ready`}
                  className="relative h-full rounded-sm bg-white/10 overflow-hidden"
                >
                  <div
                    className={`h-full ${style.stripBg} transition-all duration-500`}
                    style={{ width: `${Math.max(6, p.overallPrepPct)}%`, opacity: p.overallPrepPct > 0 ? 0.95 : 0.35 }}
                  />
                </div>
              );
            })}
          </div>
          <div className="grid grid-cols-4 gap-2 text-[11px]">
            {portions.map((p) => (
              <div key={p.code} className="flex items-center justify-between text-slate-300">
                <span className={`font-semibold ${ACCENT_MAP[p.code].text}`}>
                  {p.partLabel} ({p.weightPercent}%)
                </span>
                <span className="text-slate-400">{p.overallPrepPct}%</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* 2. Official 4-Portion Syllabus Weightage & Live Mastery Matrix */}
      <div className="grid md:grid-cols-2 gap-4">
        {portions.map((p) => {
          const style = ACCENT_MAP[p.code];
          const status = readinessLabel(p.overallPrepPct);

          return (
            <div
              key={p.code}
              className={`glass rounded-3xl p-5 border ${style.border} flex flex-col justify-between gap-4`}
            >
              <div className="space-y-3">
                {/* Header Row */}
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <div className="flex flex-wrap items-center gap-1.5">
                      <span
                        className={`text-[11px] font-bold px-2.5 py-0.5 rounded-full border ${style.badgeBg}`}
                      >
                        {p.partLabel} • {p.weightPercent}% Exam Weight
                      </span>
                      {p.isHighYield && (
                        <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-amber-400/20 text-amber-200 border border-amber-300/35">
                          HIGH-YIELD
                        </span>
                      )}
                      <span className="text-[11px] text-slate-400 font-medium">{p.units}</span>
                    </div>
                    <h3 className="text-lg font-bold mt-1.5 text-white">{p.title}</h3>
                  </div>

                  <div className="text-right shrink-0">
                    <div className={`text-2xl font-black ${style.text}`}>{p.overallPrepPct}%</div>
                    <div className="text-[11px] text-slate-400">
                      Est. <b className="text-white">{p.projectedMarks}</b>/{p.weightPercent}m
                    </div>
                  </div>
                </div>

                {/* Overall Portion Progress Bar + Status */}
                <div className="space-y-1.5">
                  <div className="flex items-center justify-between text-[11px]">
                    <span className="text-slate-300 font-medium">Preparation Till Now</span>
                    <span className={`px-2 py-0.5 rounded-full border font-semibold ${status.cls}`}>
                      {status.label}
                    </span>
                  </div>
                  <div className="w-full h-2.5 rounded-full bg-white/10 overflow-hidden">
                    <div
                      className={`h-full ${style.barBg} rounded-full transition-all duration-500`}
                      style={{ width: `${p.overallPrepPct}%` }}
                    />
                  </div>
                </div>

                {/* 3 Detailed Telemetry Metrics: Concepts, Plan, Test Marks */}
                <div className="grid grid-cols-3 gap-2 pt-1">
                  <div className="rounded-xl bg-black/25 border border-white/10 p-2.5">
                    <div className="text-[10px] uppercase text-slate-400">Concepts</div>
                    <div className="text-sm font-bold text-slate-100 mt-0.5">{p.conceptsPct}%</div>
                    <div className="w-full h-1.5 rounded-full bg-white/10 mt-1 overflow-hidden">
                      <div
                        className="h-full bg-cyan-400 rounded-full"
                        style={{ width: `${p.conceptsPct}%` }}
                      />
                    </div>
                  </div>

                  <div className="rounded-xl bg-black/25 border border-white/10 p-2.5">
                    <div className="text-[10px] uppercase text-slate-400">Plan Done</div>
                    <div className="text-sm font-bold text-slate-100 mt-0.5">{p.planPct}%</div>
                    <div className="w-full h-1.5 rounded-full bg-white/10 mt-1 overflow-hidden">
                      <div
                        className="h-full bg-violet-400 rounded-full"
                        style={{ width: `${p.planPct}%` }}
                      />
                    </div>
                  </div>

                  <div className="rounded-xl bg-black/25 border border-white/10 p-2.5">
                    <div className="text-[10px] uppercase text-slate-400">Test Marks</div>
                    <div className="text-sm font-bold text-slate-100 mt-0.5">
                      {p.testMarksPct !== null ? `${p.testMarksPct}%` : 'Pending'}
                    </div>
                    <div className="w-full h-1.5 rounded-full bg-white/10 mt-1 overflow-hidden">
                      <div
                        className="h-full bg-emerald-400 rounded-full"
                        style={{ width: `${p.testMarksPct ?? 0}%` }}
                      />
                    </div>
                  </div>
                </div>

                {/* 2025 Syllabus Highlight + Key Topics */}
                <div className="rounded-xl bg-white/5 px-3 py-2 text-[11px] text-slate-300 space-y-1">
                  <div className="flex items-center gap-1.5 text-cyan-200 font-semibold">
                    <Sparkles size={12} /> 2025 Additions: {p.new2025Highlight}
                  </div>
                  <div className="flex flex-wrap gap-1 pt-0.5">
                    {p.topics.slice(0, 4).map((t) => (
                      <span
                        key={t}
                        className="px-2 py-0.5 rounded-md bg-black/30 border border-white/10 text-[10px] text-slate-300"
                      >
                        {t}
                      </span>
                    ))}
                  </div>
                </div>
              </div>

              {/* Action Buttons */}
              <div className="grid grid-cols-2 gap-2 pt-1">
                <button
                  type="button"
                  onClick={() => onStudyPortion(p)}
                  className="px-3 py-2.5 rounded-xl bg-white/10 hover:bg-white/15 text-xs font-semibold text-white flex items-center justify-center gap-1.5 min-h-[44px]"
                >
                  <BookOpen size={14} /> Study Concepts
                </button>
                <button
                  type="button"
                  onClick={() => onPracticePortion(p.linkedBankFilter)}
                  className="px-3 py-2.5 rounded-xl bg-cyan-500/20 hover:bg-cyan-500/30 border border-cyan-300/30 text-xs font-bold text-cyan-100 flex items-center justify-center gap-1.5 min-h-[44px]"
                >
                  <Play size={14} /> Test {p.partLabel}
                </button>
              </div>
            </div>
          );
        })}
      </div>

      {/* 3. Weekly Study Consistency Line Chart (Recharts) */}
      <div
        className="glass rounded-3xl p-5 space-y-4"
        data-testid="weekly_study_consistency_card"
      >
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <div className="text-xs uppercase tracking-wider text-cyan-300 font-semibold flex items-center gap-1.5">
              <Clock size={14} /> Current Week Focus Trend • {weekRangeLabel}
            </div>
            <h3 className="text-lg font-bold mt-0.5">Weekly Study Consistency</h3>
            <p className="text-xs text-slate-400 mt-0.5">
              Total study minutes logged per day this week from Pomodoro blocks, graded tests, and manual sessions.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <button
              type="button"
              onClick={() => onLogStudyMinutes(15, 'Quick 15m reading')}
              data-testid="log_study_15m_button"
              className="px-3 py-2 rounded-xl bg-white/10 hover:bg-cyan-500/20 border border-white/15 text-xs font-semibold flex items-center gap-1 min-h-[40px]"
            >
              <Plus size={13} /> +15m Study
            </button>
            <button
              type="button"
              onClick={() => onLogStudyMinutes(25, '25m focus block')}
              data-testid="log_study_25m_button"
              className="px-3 py-2 rounded-xl bg-cyan-500/20 hover:bg-cyan-500/30 border border-cyan-300/30 text-xs font-bold text-cyan-100 flex items-center gap-1 min-h-[40px]"
            >
              <Plus size={13} /> +25m Block
            </button>
          </div>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
          <div className="rounded-2xl bg-white/5 border border-white/10 px-3.5 py-2.5">
            <div className="text-[10px] uppercase tracking-wider text-slate-400">This Week Total</div>
            <div className="text-lg font-extrabold text-cyan-200 mt-0.5">{weeklyMinutesTotal}m</div>
          </div>
          <div className="rounded-2xl bg-white/5 border border-white/10 px-3.5 py-2.5">
            <div className="text-[10px] uppercase tracking-wider text-slate-400">Today Logged</div>
            <div className="text-lg font-extrabold text-emerald-200 mt-0.5">
              {todayEntry?.minutes || 0}m
            </div>
          </div>
          <div className="rounded-2xl bg-white/5 border border-white/10 px-3.5 py-2.5">
            <div className="text-[10px] uppercase tracking-wider text-slate-400">Daily Avg (Elapsed)</div>
            <div className="text-lg font-extrabold text-violet-200 mt-0.5">{weeklyDailyAverage}m</div>
          </div>
          <div className="rounded-2xl bg-white/5 border border-white/10 px-3.5 py-2.5">
            <div className="text-[10px] uppercase tracking-wider text-slate-400">Active Days</div>
            <div className="text-lg font-extrabold text-amber-200 mt-0.5">{activeWeekDays} / 7</div>
          </div>
        </div>

        <div className="h-64 w-full">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={weeklySeries} margin={{ top: 10, right: 16, left: 0, bottom: 4 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(148,163,184,0.22)" vertical={false} />
              <XAxis
                dataKey="dayShort"
                stroke="#64748b"
                tick={{ fontSize: 12, fill: '#64748b' }}
              />
              <YAxis
                stroke="#64748b"
                tick={{ fontSize: 11, fill: '#64748b' }}
                unit="m"
                allowDecimals={false}
                width={44}
              />
              <Tooltip
                labelFormatter={(_label, payload) =>
                  payload?.[0]?.payload?.dateLabel
                    ? `${payload[0].payload.dateLabel}${payload[0].payload.isToday ? ' (Today)' : ''}`
                    : ''
                }
                formatter={(value) => [`${value ?? 0} minutes`, 'Study Logged']}
                contentStyle={{
                  backgroundColor: '#0f172a',
                  borderColor: 'rgba(56,189,248,0.45)',
                  borderRadius: '12px',
                  fontSize: '12px',
                  color: '#f8fafc'
                }}
                itemStyle={{ color: '#38bdf8', fontWeight: 700 }}
                labelStyle={{ color: '#e2e8f0', fontWeight: 700, marginBottom: 4 }}
              />
              <Legend wrapperStyle={{ fontSize: '11px' }} />
              <ReferenceLine
                y={120}
                stroke="#f59e0b"
                strokeDasharray="5 4"
                label={{
                  value: '120m Target',
                  position: 'insideTopRight',
                  fill: '#f59e0b',
                  fontSize: 10
                }}
              />
              <Line
                type="monotone"
                dataKey="minutes"
                name="Daily Study Minutes"
                stroke="#0ea5e9"
                strokeWidth={3}
                dot={{ r: 5, fill: '#0ea5e9', stroke: '#ffffff', strokeWidth: 2 }}
                activeDot={{ r: 7, fill: '#38bdf8', stroke: '#0f172a', strokeWidth: 2 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* 4. Weightage vs Preparation Chart & High-Yield 2025 Watchlist */}
      <div className="grid lg:grid-cols-[1.35fr_1fr] gap-5">
        <div className="glass rounded-3xl p-5">
          <div className="flex items-center justify-between gap-2">
            <div>
              <div className="text-xs uppercase tracking-wider text-cyan-300 font-semibold flex items-center gap-1.5">
                <TrendingUp size={14} /> Exam Weightage vs Preparation & Test Marks
              </div>
              <h3 className="text-base font-bold mt-0.5">
                Portion Performance Comparison
              </h3>
            </div>
            <span className="text-[11px] text-slate-400">Live telemetry</span>
          </div>
          <div className="h-60 mt-4">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={chartData}>
                <XAxis dataKey="name" stroke="#94a3b8" tick={{ fontSize: 11 }} />
                <YAxis stroke="#94a3b8" domain={[0, 100]} tick={{ fontSize: 11 }} />
                <Tooltip
                  contentStyle={{
                    backgroundColor: '#0f172a',
                    borderColor: 'rgba(148,163,184,0.25)',
                    borderRadius: '12px',
                    fontSize: '12px',
                    color: '#f8fafc'
                  }}
                />
                <Legend wrapperStyle={{ fontSize: '11px' }} />
                <Bar dataKey="Weight" name="Exam Weight %" fill="#f59e0b" radius={[5, 5, 0, 0]} />
                <Bar dataKey="Preparation" name="Prep Till Now %" fill="#22d3ee" radius={[5, 5, 0, 0]} />
                <Bar dataKey="TestMarks" name="Test Marks %" fill="#34d399" radius={[5, 5, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="glass rounded-3xl p-5 space-y-3">
          <div className="text-xs uppercase tracking-wider text-amber-300 font-semibold">
            2025 Scheme Focus & Weak Queue
          </div>
          <div className="rounded-2xl bg-amber-500/10 border border-amber-400/25 p-3 text-xs text-amber-100 space-y-1">
            <div className="font-bold">Exam Strategy Note (ICMA M3 2025):</div>
            <p className="text-slate-200 leading-relaxed">
              Operations (<b>35%</b>) + Marketing (<b>25%</b>) = <b>60% of marks</b>. Master ESG,
              AI quality systems, algorithmic HR bias & Web3/metaverse branding alongside Q.1–Q.10 past papers.
            </p>
          </div>
          <div className="space-y-2 pt-1">
            {weakTopics.slice(0, 4).map((w) => (
              <button
                key={w}
                type="button"
                onClick={() => onOpenRepairTopic(w)}
                className="w-full p-3 rounded-xl bg-white/5 hover:bg-cyan-400/10 border border-white/10 text-left flex items-center justify-between text-xs min-h-[44px]"
              >
                <span>
                  <b className="text-rose-300">REPAIR</b>{' '}
                  <span className="text-slate-100 font-medium">{w}</span>
                </span>
                <span className="text-cyan-300 font-semibold">Study →</span>
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
