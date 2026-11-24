import React, { useEffect, useRef, useState } from 'react';
import {
  BookOpen,
  CheckCircle2,
  ExternalLink,
  FileText,
  Highlighter,
  Layers,
  ListChecks,
  Maximize2,
  Minimize2,
  Play,
  Save,
  Sparkles,
  Video,
  X
} from 'lucide-react';
import { STUDY_MATERIALS } from '../data/icmapData';
import { StudyMaterialItem } from '../types';

interface PdfAndReaderViewProps {
  selectedMaterial: StudyMaterialItem;
  onSelectMaterial: (mat: StudyMaterialItem) => void;
  readProgressMap: Record<string, number>;
  savedNotesMap: Record<string, string>;
  onMarkRead: (materialId: string) => void;
  onSaveNotes: (materialId: string, notes: string) => void;
  onRunSelectionAi: (
    action: 'summarize_bullets' | 'explain_concept',
    selectedText: string,
    material: StudyMaterialItem
  ) => Promise<string>;
  onSendToTutor: (prompt: string, reply: string) => void;
  onGenerateFlashcards: (materialId: string) => void;
  onLaunchQuiz: (topic: string) => void;
  videoQuery: string;
  onChangeVideoQuery: (q: string) => void;
  videos: Array<{ id: string; title: string; channel: string; thumbnail: string }>;
  onFindVideos: () => void;
  isFullScreen: boolean;
  onToggleFullScreen: () => void;
}

export const PdfAndReaderView: React.FC<PdfAndReaderViewProps> = ({
  selectedMaterial,
  onSelectMaterial,
  readProgressMap,
  savedNotesMap,
  onMarkRead,
  onSaveNotes,
  onRunSelectionAi,
  onSendToTutor,
  onGenerateFlashcards,
  onLaunchQuiz,
  videoQuery,
  onChangeVideoQuery,
  videos,
  onFindVideos,
  isFullScreen,
  onToggleFullScreen
}) => {
  const [viewMode, setViewMode] = useState<'notes' | 'pdf' | 'video'>('notes');
  const [pdfPage, setPdfPage] = useState<number>(1);
  const [selectedText, setSelectedText] = useState('');
  const [aiResult, setAiResult] = useState<string>('');
  const [aiActionTitle, setAiActionTitle] = useState<string>('');
  const [aiLoading, setAiLoading] = useState(false);
  const [aiError, setAiError] = useState('');
  const [notesDraft, setNotesDraft] = useState(savedNotesMap[selectedMaterial.id] || '');
  const [customPassage, setCustomPassage] = useState('');
  const [showCustomAiInput, setShowCustomAiInput] = useState(false);

  const readerArticleRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    setNotesDraft(savedNotesMap[selectedMaterial.id] || '');
    setPdfPage(1);
    setSelectedText('');
  }, [selectedMaterial.id, savedNotesMap]);

  // Track highlighted text selection inside the reading area
  useEffect(() => {
    function handleSelection() {
      const sel = window.getSelection();
      if (!sel || sel.isCollapsed) return;
      const anchor = sel.anchorNode;
      if (readerArticleRef.current && anchor && readerArticleRef.current.contains(anchor)) {
        const text = sel.toString().trim();
        if (text.length >= 4) {
          setSelectedText(text.slice(0, 2500));
        }
      }
    }
    document.addEventListener('selectionchange', handleSelection);
    return () => document.removeEventListener('selectionchange', handleSelection);
  }, []);

  async function triggerSelectionAi(
    action: 'summarize_bullets' | 'explain_concept',
    passage: string
  ) {
    const clean = passage.trim();
    if (!clean) return;
    setAiLoading(true);
    setAiError('');
    setAiActionTitle(
      action === 'summarize_bullets' ? 'Bullet-Point Summary Notes' : 'Complex Concept Explanation'
    );
    try {
      const response = await onRunSelectionAi(action, clean, selectedMaterial);
      setAiResult(response);
    } catch (e: any) {
      setAiError(e?.message || 'AI Tutor could not analyze the selected text.');
    } finally {
      setAiLoading(false);
    }
  }

  function appendAiToNotes() {
    if (!aiResult.trim()) return;
    const prefix = notesDraft.trim() ? `${notesDraft.trim()}\n\n` : '';
    const updated = `${prefix}--- AI ${aiActionTitle} ---\n${aiResult.trim()}`;
    setNotesDraft(updated);
    onSaveNotes(selectedMaterial.id, updated);
  }

  const pct = readProgressMap[selectedMaterial.id] || 0;
  const pdfSrcWithPage = selectedMaterial.pdfUrl
    ? `${selectedMaterial.pdfUrl}#page=${pdfPage}`
    : '';

  return (
    <div className="space-y-5">
      {/* Material Selector + Full-Screen Header */}
      <div className="glass rounded-3xl p-5">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div className="flex-1 min-w-[220px]">
            <div className="text-xs uppercase tracking-widest text-cyan-300 font-semibold">
              {selectedMaterial.category}
            </div>
            <h2 className="text-xl md:text-2xl font-bold mt-1">{selectedMaterial.title}</h2>
            <p className="text-xs text-slate-400 mt-1">{selectedMaterial.description}</p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <button
              type="button"
              onClick={() => onMarkRead(selectedMaterial.id)}
              className="px-3.5 py-2.5 rounded-xl bg-emerald-500/20 hover:bg-emerald-500/30 text-emerald-100 text-xs font-semibold flex items-center gap-1.5 min-h-[44px]"
            >
              <CheckCircle2 size={15} /> Mark Read ({pct}%)
            </button>
            <button
              type="button"
              onClick={onToggleFullScreen}
              aria-label={isFullScreen ? 'Exit full-screen reader' : 'Full-screen reader'}
              className="px-3.5 py-2.5 rounded-xl bg-white/5 hover:bg-white/10 text-xs font-semibold flex items-center gap-1.5 min-h-[44px]"
            >
              {isFullScreen ? <Minimize2 size={15} /> : <Maximize2 size={15} />}
              {isFullScreen ? 'Exit Full Screen' : 'Full Screen'}
            </button>
          </div>
        </div>

        {/* Material Switcher Pills */}
        <div className="flex gap-2 overflow-x-auto pb-1 mt-4 scrollbar">
          {STUDY_MATERIALS.map((mat) => (
            <button
              key={mat.id}
              type="button"
              onClick={() => onSelectMaterial(mat)}
              className={`px-3 py-1.5 rounded-full text-xs whitespace-nowrap border transition ${
                selectedMaterial.id === mat.id
                  ? 'bg-cyan-400/20 text-cyan-200 border-cyan-300/40'
                  : 'bg-white/5 text-slate-400 border-white/10 hover:text-white'
              }`}
            >
              {mat.title}
            </button>
          ))}
        </div>

        {/* View Mode Switcher: Interactive Text / Embedded PDF / Video Clip */}
        <div className="flex flex-wrap gap-2 mt-4">
          <button
            type="button"
            onClick={() => setViewMode('notes')}
            className={`px-3.5 py-2 rounded-xl text-xs font-semibold flex items-center gap-1.5 min-h-[40px] ${
              viewMode === 'notes' ? 'bg-cyan-500/25 text-cyan-100' : 'bg-white/5 text-slate-300'
            }`}
          >
            <BookOpen size={14} /> Selectable Study Text
          </button>
          {selectedMaterial.pdfUrl && (
            <button
              type="button"
              onClick={() => setViewMode('pdf')}
              className={`px-3.5 py-2 rounded-xl text-xs font-semibold flex items-center gap-1.5 min-h-[40px] ${
                viewMode === 'pdf' ? 'bg-violet-500/25 text-violet-100' : 'bg-white/5 text-slate-300'
              }`}
            >
              <FileText size={14} /> Embedded PDF Book
            </button>
          )}
          <button
            type="button"
            onClick={() => setViewMode('video')}
            className={`px-3.5 py-2 rounded-xl text-xs font-semibold flex items-center gap-1.5 min-h-[40px] ${
              viewMode === 'video' ? 'bg-rose-500/25 text-rose-100' : 'bg-white/5 text-slate-300'
            }`}
          >
            <Video size={14} /> Lecture Videos
          </button>
          {selectedMaterial.externalUrl && (
            <a
              href={selectedMaterial.externalUrl}
              target="_blank"
              rel="noreferrer"
              className="px-3.5 py-2 rounded-xl text-xs font-semibold bg-white/5 hover:bg-white/10 text-cyan-200 flex items-center gap-1.5 min-h-[40px]"
            >
              <ExternalLink size={14} /> Open Drive
            </a>
          )}
        </div>

        <div className="progress mt-4">
          <span style={{ width: `${pct}%` }} />
        </div>
      </div>

      {/* Main Content Workspace */}
      {viewMode === 'pdf' && selectedMaterial.pdfUrl ? (
        <div className="glass rounded-3xl p-4 md:p-5 space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div className="flex items-center gap-2 flex-wrap">
              {selectedMaterial.chapters && (
                <select
                  value={pdfPage}
                  onChange={(e) => setPdfPage(Number(e.target.value))}
                  className="bg-black/35 border border-white/15 rounded-xl px-3 py-2 text-xs text-slate-100 max-w-full"
                >
                  {selectedMaterial.chapters.map((ch) => (
                    <option key={ch.title} value={ch.pageNumber} className="bg-[#07111f]">
                      p.{ch.pageNumber} — {ch.title}
                    </option>
                  ))}
                </select>
              )}
              <button
                type="button"
                onClick={() => setShowCustomAiInput((v) => !v)}
                className="px-3 py-2 rounded-xl bg-cyan-500/20 text-cyan-100 text-xs font-semibold flex items-center gap-1.5"
              >
                <Sparkles size={14} /> Paste PDF Passage for AI Notes
              </button>
            </div>
            <a
              href={pdfSrcWithPage}
              target="_blank"
              rel="noreferrer"
              className="px-3 py-2 rounded-xl bg-white/5 hover:bg-white/10 text-xs text-slate-200 flex items-center gap-1.5"
            >
              <ExternalLink size={14} /> Open PDF Tab
            </a>
          </div>

          {showCustomAiInput && (
            <div className="p-3.5 rounded-2xl bg-black/30 border border-cyan-400/20 space-y-2">
              <textarea
                value={customPassage}
                onChange={(e) => setCustomPassage(e.target.value)}
                rows={3}
                placeholder="Copy/paste a paragraph or concept from the PDF page here to summarize or explain..."
                className="w-full rounded-xl bg-white/5 border border-white/10 p-3 text-xs"
              />
              <div className="flex flex-wrap gap-2">
                <button
                  type="button"
                  disabled={aiLoading || !customPassage.trim()}
                  onClick={() => triggerSelectionAi('summarize_bullets', customPassage)}
                  className="px-3.5 py-2 rounded-xl bg-cyan-500/25 text-cyan-100 text-xs font-semibold"
                >
                  <ListChecks size={14} className="inline mr-1" /> Bullet Notes
                </button>
                <button
                  type="button"
                  disabled={aiLoading || !customPassage.trim()}
                  onClick={() => triggerSelectionAi('explain_concept', customPassage)}
                  className="px-3.5 py-2 rounded-xl bg-violet-500/25 text-violet-100 text-xs font-semibold"
                >
                  <Sparkles size={14} className="inline mr-1" /> Explain Concept
                </button>
              </div>
            </div>
          )}

          <div className="rounded-2xl overflow-hidden border border-white/10 bg-black/40">
            <iframe
              key={pdfSrcWithPage}
              src={pdfSrcWithPage}
              title={selectedMaterial.title}
              className={`w-full ${isFullScreen ? 'h-[78vh]' : 'h-[62vh]'}`}
            />
          </div>
        </div>
      ) : viewMode === 'video' ? (
        <div className="glass rounded-3xl p-5 space-y-4">
          <div className="flex items-center justify-between gap-2">
            <h3 className="font-bold text-base flex items-center gap-2">
              <Video size={18} className="text-rose-300" /> Topic Lecture Search & Player
            </h3>
            <button
              type="button"
              onClick={onToggleFullScreen}
              className="px-3 py-2 rounded-xl bg-white/5 text-xs flex items-center gap-1"
            >
              {isFullScreen ? <Minimize2 size={14} /> : <Maximize2 size={14} />}
              {isFullScreen ? 'Exit Full Screen' : 'Full-Screen Video Mode'}
            </button>
          </div>
          <div className="flex gap-2">
            <input
              value={videoQuery}
              onChange={(e) => onChangeVideoQuery(e.target.value)}
              placeholder="Search ICMA OM topic lectures (e.g. Balanced Scorecard, Mintzberg 5 parts)"
              className="flex-1 bg-black/30 border border-white/10 rounded-xl px-3.5 py-2.5 text-sm"
            />
            <button
              type="button"
              onClick={onFindVideos}
              className="px-4 py-2.5 rounded-xl bg-rose-500/25 hover:bg-rose-500/35 text-rose-100 text-xs font-semibold flex items-center gap-1.5"
            >
              <Play size={14} /> Search YouTube
            </button>
          </div>
          {videos.length > 0 ? (
            <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-4">
              {videos.map((v) => (
                <a
                  key={v.id}
                  href={`https://www.youtube.com/watch?v=${v.id}`}
                  target="_blank"
                  rel="noreferrer"
                  className="rounded-2xl overflow-hidden bg-black/30 border border-white/10 hover:border-cyan-400/35 transition"
                >
                  <img src={v.thumbnail} alt={v.title} className="w-full aspect-video object-cover" />
                  <div className="p-3 text-xs font-medium">{v.title}</div>
                  <div className="px-3 pb-3 text-[11px] text-slate-400">{v.channel}</div>
                </a>
              ))}
            </div>
          ) : (
            <div className="p-5 rounded-2xl bg-white/5 text-xs text-slate-300">
              Click <strong>Search YouTube</strong> to find lecture walkthroughs (opens embedded results when{' '}
              <code>YOUTUBE_API_KEY</code> is configured or launches direct YouTube search).
            </div>
          )}
        </div>
      ) : (
        <div className="grid lg:grid-cols-[1fr_340px] gap-5">
          {/* Selectable Reading Pane */}
          <div className="glass rounded-3xl p-5 md:p-6 space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-2 pb-3 border-b border-white/10">
              <div className="text-xs text-cyan-200 flex items-center gap-1.5">
                <Highlighter size={15} />
                Highlight any text below to generate AI Bullet Notes or Explanations
              </div>
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => onLaunchQuiz(selectedMaterial.title)}
                  className="px-3 py-1.5 rounded-xl bg-cyan-500/15 hover:bg-cyan-500/25 text-xs font-medium"
                >
                  Quiz Topic
                </button>
                <button
                  type="button"
                  onClick={() => onGenerateFlashcards(selectedMaterial.id)}
                  className="px-3 py-1.5 rounded-xl bg-violet-500/15 hover:bg-violet-500/25 text-xs font-medium flex items-center gap-1"
                >
                  <Layers size={13} /> Make Flashcards
                </button>
              </div>
            </div>

            {/* Active Selection Bar */}
            {selectedText && (
              <div className="p-3.5 rounded-2xl bg-cyan-500/12 border border-cyan-300/35 space-y-2.5">
                <div className="flex items-start justify-between gap-2">
                  <div className="text-xs text-cyan-100">
                    <span className="font-semibold">Selected:</span> “
                    {selectedText.slice(0, 140)}
                    {selectedText.length > 140 ? '…' : ''}”
                  </div>
                  <button
                    type="button"
                    onClick={() => setSelectedText('')}
                    aria-label="Clear selection"
                    className="text-slate-400 hover:text-white"
                  >
                    <X size={15} />
                  </button>
                </div>
                <div className="flex flex-wrap gap-2">
                  <button
                    type="button"
                    disabled={aiLoading}
                    onClick={() => triggerSelectionAi('summarize_bullets', selectedText)}
                    className="px-3.5 py-2 rounded-xl bg-cyan-500/25 hover:bg-cyan-500/35 text-cyan-100 text-xs font-semibold flex items-center gap-1.5 min-h-[40px]"
                  >
                    <ListChecks size={14} /> Bullet-Point Notes
                  </button>
                  <button
                    type="button"
                    disabled={aiLoading}
                    onClick={() => triggerSelectionAi('explain_concept', selectedText)}
                    className="px-3.5 py-2 rounded-xl bg-violet-500/25 hover:bg-violet-500/35 text-violet-100 text-xs font-semibold flex items-center gap-1.5 min-h-[40px]"
                  >
                    <Sparkles size={14} /> Explain Complex Concept
                  </button>
                </div>
              </div>
            )}

            <div
              ref={readerArticleRef}
              className="whitespace-pre-wrap text-sm md:text-[15px] leading-7 text-slate-200 select-text"
            >
              {selectedMaterial.readingContent}
            </div>
          </div>

          {/* Right Rail: AI Selection Output + Personal Study Notes */}
          <div className="space-y-4">
            {(aiLoading || aiResult || aiError) && (
              <div className="glass rounded-3xl p-4 border-cyan-300/30 space-y-3">
                <div className="flex items-center justify-between">
                  <div className="text-xs uppercase tracking-wider text-cyan-300 font-semibold flex items-center gap-1.5">
                    <Sparkles size={14} /> {aiActionTitle || 'AI Selection Tutor'}
                  </div>
                  <button
                    type="button"
                    onClick={() => {
                      setAiResult('');
                      setAiError('');
                    }}
                    className="text-slate-400 hover:text-white"
                  >
                    <X size={15} />
                  </button>
                </div>

                {aiLoading ? (
                  <div className="text-xs text-slate-300 py-4">Analyzing highlighted passage…</div>
                ) : aiError ? (
                  <div className="text-xs text-rose-300">{aiError}</div>
                ) : (
                  <>
                    <div className="whitespace-pre-wrap text-xs leading-6 text-slate-200 max-h-72 overflow-y-auto scrollbar">
                      {aiResult}
                    </div>
                    <div className="flex flex-wrap gap-2 pt-1">
                      <button
                        type="button"
                        onClick={appendAiToNotes}
                        className="flex-1 px-3 py-2 rounded-xl bg-emerald-500/20 text-emerald-100 text-xs font-semibold flex items-center justify-center gap-1"
                      >
                        <Save size={13} /> Save to Notes
                      </button>
                      <button
                        type="button"
                        onClick={() =>
                          onSendToTutor(
                            `Follow up on ${selectedMaterial.title}: ${selectedText.slice(0, 220)}`,
                            aiResult
                          )
                        }
                        className="flex-1 px-3 py-2 rounded-xl bg-cyan-500/20 text-cyan-100 text-xs font-semibold"
                      >
                        Follow Up
                      </button>
                    </div>
                  </>
                )}
              </div>
            )}

            <div className="glass rounded-3xl p-4 space-y-3">
              <div className="text-xs uppercase tracking-wider text-cyan-300 font-semibold">
                Personal Study Notes (Saved Locally)
              </div>
              <textarea
                value={notesDraft}
                onChange={(e) => setNotesDraft(e.target.value)}
                rows={8}
                placeholder="Write exam formulas, mnemonics, or save AI bullet notes here..."
                className="w-full rounded-2xl bg-black/30 border border-white/10 p-3 text-xs leading-6"
              />
              <button
                type="button"
                onClick={() => onSaveNotes(selectedMaterial.id, notesDraft)}
                className="w-full py-2.5 rounded-xl bg-cyan-500/20 hover:bg-cyan-500/30 text-cyan-100 text-xs font-semibold flex items-center justify-center gap-1.5 min-h-[44px]"
              >
                <Save size={14} /> Save Notes
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
