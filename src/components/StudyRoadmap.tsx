import React from 'react';
import {
  CheckCircle2,
  Circle,
  Compass,
  Flag,
  Maximize2,
  Minimize2,
  Play
} from 'lucide-react';
import { ROADMAP_MILESTONES } from '../data/icmapData';
import { RoadmapMilestone } from '../types';

interface StudyRoadmapProps {
  completedMilestoneIds: string[];
  onToggleMilestone: (id: string) => void;
  onStartMilestone: (milestone: RoadmapMilestone) => void;
  isFullScreen: boolean;
  onToggleFullScreen: () => void;
}

export const StudyRoadmap: React.FC<StudyRoadmapProps> = ({
  completedMilestoneIds,
  onToggleMilestone,
  onStartMilestone,
  isFullScreen,
  onToggleFullScreen
}) => {
  const completedCount = ROADMAP_MILESTONES.filter((m) =>
    completedMilestoneIds.includes(m.id)
  ).length;
  const pct = Math.round((completedCount / ROADMAP_MILESTONES.length) * 100);
  const nextUpcoming = ROADMAP_MILESTONES.find(
    (m) => !completedMilestoneIds.includes(m.id)
  );

  return (
    <div className="glass rounded-3xl p-5">
      <div className="flex items-start justify-between gap-3">
        <div>
          <div className="flex items-center gap-2 text-xs uppercase tracking-widest text-cyan-300 font-semibold">
            <Compass size={15} /> Visual Study Roadmap
          </div>
          <h3 className="text-lg font-bold mt-1">
            OM M3 Mastery Track ({completedCount}/{ROADMAP_MILESTONES.length} Milestones)
          </h3>
          {nextUpcoming && (
            <p className="text-xs text-slate-400 mt-0.5 flex items-center gap-1.5">
              <Flag size={13} className="text-amber-300 shrink-0" />
              Next milestone: <span className="text-amber-200 font-medium">{nextUpcoming.title}</span>
            </p>
          )}
        </div>
        <button
          type="button"
          onClick={onToggleFullScreen}
          aria-label={isFullScreen ? 'Exit full-screen roadmap' : 'Full-screen roadmap'}
          className="p-2.5 rounded-xl bg-white/5 hover:bg-white/10 min-w-[44px] min-h-[44px] flex items-center justify-center"
        >
          {isFullScreen ? <Minimize2 size={17} /> : <Maximize2 size={17} />}
        </button>
      </div>

      <div className="mt-4">
        <div className="flex justify-between text-xs text-slate-400 mb-1.5">
          <span>Overall Module Completion</span>
          <span className="text-cyan-200 font-semibold">{pct}%</span>
        </div>
        <div className="progress">
          <span style={{ width: `${pct}%` }} />
        </div>
      </div>

      <div className="mt-5 space-y-3">
        {ROADMAP_MILESTONES.map((ms, index) => {
          const isDone = completedMilestoneIds.includes(ms.id);
          const isNext = nextUpcoming?.id === ms.id;

          return (
            <div
              key={ms.id}
              className={`relative rounded-2xl p-4 border transition ${
                isDone
                  ? 'bg-emerald-500/10 border-emerald-400/25'
                  : isNext
                    ? 'bg-cyan-500/12 border-cyan-400/35'
                    : 'bg-white/5 border-white/10'
              }`}
            >
              <div className="flex items-start justify-between gap-3">
                <button
                  type="button"
                  onClick={() => onToggleMilestone(ms.id)}
                  className="flex items-start gap-3 text-left flex-1 min-h-[44px]"
                >
                  <div className="mt-0.5 shrink-0">
                    {isDone ? (
                      <CheckCircle2 size={20} className="text-emerald-300" />
                    ) : (
                      <Circle
                        size={20}
                        className={isNext ? 'text-cyan-300' : 'text-slate-500'}
                      />
                    )}
                  </div>
                  <div>
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="text-[11px] uppercase tracking-wider font-semibold text-cyan-300">
                        {ms.unitLabel}
                      </span>
                      {isNext && (
                        <span className="text-[10px] px-2 py-0.5 rounded-full bg-amber-400/20 text-amber-200 font-semibold">
                          UPCOMING TARGET
                        </span>
                      )}
                      {isDone && (
                        <span className="text-[10px] px-2 py-0.5 rounded-full bg-emerald-400/20 text-emerald-200 font-semibold">
                          COMPLETED
                        </span>
                      )}
                    </div>
                    <div className="font-semibold text-sm mt-1">
                      {index + 1}. {ms.title}
                    </div>
                    <p className="text-xs text-slate-400 mt-1">{ms.description}</p>
                  </div>
                </button>

                <button
                  type="button"
                  onClick={() => onStartMilestone(ms)}
                  className="px-3 py-2 rounded-xl bg-cyan-500/20 hover:bg-cyan-500/30 text-cyan-100 text-xs font-medium flex items-center gap-1.5 shrink-0 min-h-[44px]"
                >
                  <Play size={13} /> Study
                </button>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
