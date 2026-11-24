import {
  FlashcardItem,
  McqQuestionItem,
  PdfChapterJump,
  RoadmapMilestone,
  StudyMaterialItem,
  SubjectInfo
} from '../types';

export const SUBJECTS: SubjectInfo[] = [
  {
    code: 'OM',
    name: 'M3 Organizational Management (ML-1)',
    progress: 58,
    weak: ['Balanced Scorecard', 'Mintzberg 5 Parts', 'MRP & OPT', 'Quality Circles & TPM']
  },
  {
    code: 'FACR',
    name: 'Financial Accounting & Corporate Reporting',
    progress: 52,
    weak: ['Deferred Tax (IAS 12)', 'IAS 40 Investment Property', 'Business Combinations']
  },
  {
    code: 'BT',
    name: 'Business Taxation',
    progress: 64,
    weak: ['Income Tax Ordinance', 'Taxable Income', 'Residential Status']
  }
];

export const OM_BOOK_CHAPTERS: PdfChapterJump[] = [
  { title: 'Cover & Syllabus Overview', pageNumber: 1 },
  { title: 'Unit 1: Enterprise Mgmt, PESTEL & ESG', pageNumber: 8 },
  { title: 'Unit 2: Operations Mgmt & Mintzberg', pageNumber: 19 },
  { title: 'Unit 3: Production Techniques & 3D Printing', pageNumber: 30 },
  { title: 'Unit 4: Plant Maintenance, 5S & TPM / OEE', pageNumber: 37 },
  { title: 'Unit 5: Production Planning, MRP, ERP & OPT', pageNumber: 49 },
  { title: 'Unit 6: Operational Capacity & Queuing', pageNumber: 64 },
  { title: 'Unit 7: Quality, Six Sigma, Kaizen, Lean & ISO', pageNumber: 68 },
  { title: 'Unit 8: HRM & Motivation Theories', pageNumber: 87 },
  { title: 'Unit 9: Reliable HR (Pfeffer) & Kolb Learning', pageNumber: 112 },
  { title: 'Unit 10: Marketing Concept, STP & Virtual Branding', pageNumber: 120 },
  { title: 'Unit 11: Marketing Plans, 7Ps, BCG & Ansoff', pageNumber: 132 },
  { title: 'Unit 12: Consumer Behavior, CSR & Web3/DAO', pageNumber: 197 },
  { title: 'Unit 13: Enterprise Performance Mgmt & Budgets', pageNumber: 215 },
  { title: 'Unit 14: Balanced Scorecard, Building Block & Pyramid', pageNumber: 230 }
];

export const PAST_PAPER_CHAPTERS: PdfChapterJump[] = [
  { title: 'Page 1: Q1 Marketing, Q2 HRM, Q3 Agencies, Q4 BSC', pageNumber: 1 },
  { title: 'Page 2: Q4 BSC Drawbacks (cont.) & Q5 Sales Orientation', pageNumber: 2 },
  { title: 'Page 3: Q6 Mintzberg, Q7 Budgetary Control, Q8 Interviews', pageNumber: 3 },
  { title: 'Page 4: Q8 (cont.), Q9 Quality Circles, Q10 MRP Benefits', pageNumber: 4 }
];

export const STUDY_MATERIALS: StudyMaterialItem[] = [
  {
    id: 'om_study_text_2026',
    title: 'M3 Organizational Management — Study Text 2026',
    category: 'OM Book (ICMA M3)',
    type: 'pdf',
    description:
      'Complete 254-page ICMA Pakistan M3 Organizational Management textbook (Units 1–14). Open embedded PDF reader with chapter jumps or select text below for instant AI bullet notes.',
    pdfUrl: '/assets/aistudio/M3_Organizational_Management_bb00511f.pdf',
    estimatedMinutes: 90,
    chapters: OM_BOOK_CHAPTERS,
    readingContent: `ICMA Pakistan — M3 Organizational Management (2026 Study Text)

PART A — OPERATIONS MANAGEMENT (Units 1–7)
• Unit 1 (p.8): Enterprise Management, PESTEL macro-environment (Political, Economic, Social, Technological, Environmental, Legal), Digital Transformation, and ESG (Environmental, Social, Governance) reporting.
• Unit 2 (p.19): Operations Management & Mintzberg's 5 Organizational Parts — Operating Core (frontline producers), Strategic Apex (top executives), Middle Line (managerial chain), Technostructure (analysts standardizing processes/outputs), and Support Staff (indirect functions like legal, PR, catering).
• Unit 3 (p.30): Production Systems — Job Production (custom one-off), Batch Production (groups of identical items), Process/Flow Production (continuous high-volume), and Additive Manufacturing (3D printing layer-by-layer from CAD).
• Unit 4 (p.37): Plant Maintenance, 5S (Seiri/Sort, Seiton/Set in Order, Seiso/Shine, Seiketsu/Standardize, Shitsuke/Sustain), Total Productive Maintenance (TPM 8 Pillars), and OEE = Availability × Performance × Quality.
• Unit 5 (p.49): Production Planning & Control — MRP (Material Requirement Planning), MRP-II, ERP, and OPT / Theory of Constraints using Drum-Buffer-Rope scheduling anchored around bottleneck resources.
• Unit 6 (p.64): Managing Operational Capacity — Design vs Effective Capacity, Flexible Manufacturing Systems (FMS), Demand Forecasting, and Queuing Theory.
• Unit 7 (p.68): Quality Management — Juran's Cost of Quality (Prevention, Appraisal, Internal Failure, External Failure), Six Sigma DMAIC (3.4 DPMO), Kaizen continuous improvement, Lean/Kanban JIT, Quality Circles, TQMEX (5S → BPR → QCC → ISO 9000 → TPM), and ISO standards.

PART B — HUMAN CAPITAL MANAGEMENT (Units 8–9)
• Unit 8 (p.87): Motivation Theories (Taylor Scientific Management, Maslow Hierarchy, Herzberg Two-Factor Hygiene vs Motivators, Vroom Expectancy, McGregor Theory X/Y), Charles Handy's Shamrock Organization (Core, Contractual Fringe, Flexible Labor) & Psychological Contract, 70-20-10 Learning Model, and Interview Limitations (Halo Effect, Contagious Bias, Stereotyping, Logical Error).
• Unit 9 (p.112): Jeffrey Pfeffer's 7 Practices of Successful Organizations (Employment Security, Selective Hiring, Self-Managed Teams, High Contingent Pay, Extensive Training, Reduced Status Distinctions, Open Financial Info Sharing) and Kolb's Experiential Learning Cycle (Concrete Experience → Reflective Observation → Abstract Conceptualization → Active Experimentation).

PART C — MARKETING MANAGEMENT (Units 10–12)
• Unit 10 (p.120): Marketing Orientations (Production, Product, Sales, Marketing, Societal), Segmentation-Targeting-Positioning (STP), and Virtual/Metaverse Branding.
• Unit 11 (p.132): 4Ps & 7Ps Services Mix (+ People, Process, Physical Evidence), BCG Growth-Share Matrix (Stars, Cash Cows, Question Marks, Dogs), and Ansoff Growth Matrix (Market Penetration, Product Development, Market Development, Diversification).
• Unit 12 (p.197): Consumer Buying Decision Process (Need Recognition → Search → Evaluation → Purchase → Post-Purchase), DMU Roles, B2B vs B2C, CSR & Web3/DAO.

PART D — ENTERPRISE PERFORMANCE MANAGEMENT (Units 13–14)
• Unit 13 (p.215): EPM Control Loop, Critical Success Factors (CSFs) vs Key Performance Indicators (KPIs), and Budgetary Control (variance analysis, cost control, coordination).
• Unit 14 (p.230): Balanced Scorecard (Kaplan & Norton: Financial, Customer, Internal Business Process, Learning & Growth), Fitzgerald & Moon's Building Block Model (Dimensions, Standards, Rewards), and Lynch & Cross's Performance Pyramid.`
  },
  {
    id: 'om_past_papers_ml1',
    title: 'OM ML-1 Past Papers & Suggested Solutions',
    category: 'Past Papers (ICMA ML-1)',
    type: 'pdf',
    description:
      '10 descriptive ICMA past paper questions (5 marks each) with official suggested answers covering Marketing, HRM, BSC, Mintzberg, Quality Circles, Interviews & MRP.',
    pdfUrl: '/assets/aistudio/Past_Papers_OM-ML1_fe3df661.pdf',
    estimatedMinutes: 40,
    chapters: PAST_PAPER_CHAPTERS,
    readingContent: `Descriptive Past Paper Questions & Suggested Solutions (OM — ML1, 5 Marks Each)

Q.1 Define the marketing concept. (05)
A management orientation accepting that the key task of the organization is to determine the needs, wants, and values of a target market and adapt the organization to delivering desired satisfaction more effectively and efficiently than competitors.

Q.2 Importance of the HRM process & external influences. (05)
• Three Reasons: (1) Source of competitive advantage; (2) Vital part of organizational strategies; (3) Impacts organizational performance through how people are treated.
• Four External Factors: Economy, labor unions, legal environment, and demographic trends.

Q.3 Why outsource recruitment to private employment agencies instead of in-house? (05)
1. HR may lack trained staff for media ads & candidate screening.
2. Avoids alerting competitors to expansion plans.
3. Cost-effective screening of overwhelming applicant volume.
4. Prevents job ads from signaling high employee turnover.
5. Agencies keep ready candidate databanks for urgent hires.
6. Confidentiality for senior-level executives exploring moves via headhunters.

Q.4 Disadvantages of using a Balanced Scorecard (BSC). (05)
• Difficult to record & process qualitative (non-financial) data.
• Information overload from tracking too many measures.
• Potential conflict between measures (e.g., cutting R&D boosts short-term profit).
• Chosen metrics may misalign with strategy/vision.
• Poor communication if managers are still rewarded solely on narrow financial metrics.
• High setup/measurement cost and risk of failure without senior management commitment.

Q.5 Organizations using sales orientation & drawbacks. (05)
• Examples: Retailers (FMCG), Direct Sales/MLM companies, Telemarketing/Call centers, Real estate agencies.
• Drawbacks: Customer dissatisfaction from pushy selling, short-term focus, lack of R&D innovation, limited customer insight, and high sales-staff turnover.

Q.6 Explain Mintzberg's Effective Organization (5 Parts). (05)
1. Operating core: Frontline workers directly producing products or delivering services.
2. Strategic apex: Top executives formulating/implementing strategy to serve owners.
3. Middle line: Chain of managerial hierarchy from strategic apex to frontline supervisors.
4. Technostructure: Analysts standardizing processes, outputs & skills (e.g., accountants, HR).
5. Support staff: Indirect assistance outside operational workflow (e.g., legal, PR, catering).

Q.7 How budgetary controls help earn maximum profit? (05)
Controls costs and improves operational efficiency; measures departmental and cost-center performance; highlights cost-reduction areas; supports performance incentives; aligns inter-departmental coordination; enables variance analysis & timely corrective action.

Q.8 General limitations of job interviews. (05)
Unreliable assessments (interviewers disagree) and low predictive validity of actual job performance. Key judgment errors: (i) Halo effect, (ii) Contagious bias (leading cues), (iii) Stereotyping (dress/accent/voice), (iv) Misjudging abstract traits (honesty/motivation), and (v) Logical error.

Q.9 Define Quality Circle and explain its advantages. (05)
A small voluntary group of employees with mixed skills from all levels meeting regularly to solve real quality problems. Advantages: Higher quality, customer satisfaction & productivity; continuous improvement culture; operational staff understand shop-floor issues best; fosters unity.

Q.10 Benefits of Material Requirement Planning (MRP). (05)
Improved ability to meet customer orders; reduced inventory holding costs; rapid computerized schedule updates when demand changes; early warning of purchasing/production bottlenecks; stronger supplier coordination.`
  },
  {
    id: 'drive_folder_1',
    title: 'Core Study Folder & Lecture Pack (Google Drive)',
    category: 'Drive Resources',
    type: 'drive',
    description:
      'Curated Google Drive course folder & Master Study Guide with readings, recorded walkthrough clips, and revision handouts.',
    externalUrl: 'https://drive.google.com/drive/folders/1JRAADz_hSw7MLr4_Pl6fuhuyRhlyt3ex',
    estimatedMinutes: 35,
    readingContent: `Google Drive Course Resources & Study Workflow

Attached Course Links:
• Core Study Folder 1: https://drive.google.com/drive/folders/1JRAADz_hSw7MLr4_Pl6fuhuyRhlyt3ex
• Primary Reading File 2: https://drive.google.com/file/d/1C976hWaHp0S8gjrjMIe5M64p1IkmJ1JW/view?usp=drivesdk
• Lecture Clip & Visuals File 3: https://drive.google.com/file/d/1bKnRRG5h6hCmga7EBbN37vGbxiuMSvcS/view?usp=drivesdk
• Video Clips Archive Folder 4: https://drive.google.com/drive/folders/1trJJR26p772Twd0O7hFuzl-uRCtqDRzx
• Practice Sets Folder 5: https://drive.google.com/drive/folders/1cSJry6g8mBLEz0slwzLHyz-UyiGkvfXM
• Master Study Guide Doc 6: https://docs.google.com/document/d/1wZP3HFOlBIjIZJzw82Z_-yT7yeuuYfZYh3IVE3G3NJQ/edit?usp=drivesdk

Study Workflow:
1. Run a 25-minute Pomodoro deep focus block.
2. Read the core unit or open a Drive resource on your laptop split-screen.
3. Highlight tricky terms here to trigger Bullet Notes or Concept Explanations.
4. Generate active-recall flashcards and finish with an ICMA MCQ sprint.`
  }
];

export const OM_MCQ_QUESTIONS: McqQuestionItem[] = [
  {
    id: 'om_q1',
    bankId: 'om_ops',
    question:
      'In Mintzberg’s Effective Organization, which part consists of analysts (like management accountants and HR planners) who standardize work processes and outputs?',
    options: ['Operating Core', 'Strategic Apex', 'Technostructure', 'Support Staff'],
    answer: 2,
    explanation:
      'Mintzberg’s Technostructure standardizes processes, outputs, and skills outside the direct line of managerial authority.'
  },
  {
    id: 'om_q2',
    bankId: 'om_ops',
    question: 'How is Overall Equipment Effectiveness (OEE) calculated in Total Productive Maintenance (TPM)?',
    options: [
      'Availability + Performance + Quality',
      'Availability × Performance × Quality',
      'Uptime ÷ Total Downtime',
      'Good Units × Machine Speed'
    ],
    answer: 1,
    explanation:
      'OEE multiplies the three effectiveness ratios: Availability × Performance × Quality.'
  },
  {
    id: 'om_q3',
    bankId: 'om_ops',
    question: 'What scheduling mechanism lies at the heart of Optimized Production Technology (OPT / Theory of Constraints)?',
    options: [
      'Maximizing work-in-progress inventory everywhere',
      'Drum-Buffer-Rope scheduling anchored around bottleneck capacity',
      'Eliminating preventive maintenance checks',
      'Annual static spreadsheets'
    ],
    answer: 1,
    explanation:
      'OPT sets production pace via the bottleneck (Drum), protects it with buffer stock (Buffer), and paces material release (Rope).'
  },
  {
    id: 'om_q4',
    bankId: 'om_ops',
    question: 'What defect rate defines Six Sigma quality performance?',
    options: [
      '340 defects per million opportunities',
      '3.4 defects per million opportunities (DPMO)',
      '3.4% defective units per batch',
      '60 defects per thousand'
    ],
    answer: 1,
    explanation:
      'Six Sigma targets near-perfection with no more than 3.4 defects per million opportunities (99.99966% defect-free).'
  },
  {
    id: 'om_q5',
    bankId: 'om_ops',
    question: 'In Juran’s Cost of Quality (COQ) model, inspection, testing, and quality audit costs fall under:',
    options: ['Internal Failure Costs', 'External Failure Costs', 'Appraisal Costs', 'Prevention Costs'],
    answer: 2,
    explanation: 'Appraisal costs inspect and test output to detect defects before delivery.'
  },
  {
    id: 'om_q6',
    bankId: 'om_hrm',
    question: 'According to Herzberg’s Two-Factor Theory, which of the following acts as a Hygiene Factor rather than a Motivator?',
    options: [
      'Recognition and achievement',
      'Challenging meaningful work',
      'Company policy, salary, and working conditions',
      'Advancement and personal growth'
    ],
    answer: 2,
    explanation:
      'Hygiene factors (salary, policy, working conditions) prevent dissatisfaction when adequate, whereas motivators drive genuine satisfaction.'
  },
  {
    id: 'om_q7',
    bankId: 'om_hrm',
    question: 'Charles Handy’s "Shamrock Organization" consists of which three workforce groups?',
    options: [
      'Strategic Apex, Technostructure, Support Staff',
      'Professional Core, Contractual Fringe, Flexible Labor Force',
      'Managers, Unions, Regulators',
      'Prevention, Appraisal, Failure'
    ],
    answer: 1,
    explanation:
      'Handy’s Shamrock divides work across the Core Professionals, Contractual Fringe (outsourced), and Flexible Labor (part-time/temp).'
  },
  {
    id: 'om_q8',
    bankId: 'om_hrm',
    question: 'When an interviewer lets one standout trait (good or bad) color their entire rating of a candidate, it is called:',
    options: ['Logical Error', 'Contagious Bias', 'Halo Effect', 'Predictive Validity'],
    answer: 2,
    explanation: 'The Halo Effect allows a single dominant impression to distort the overall interview assessment.'
  },
  {
    id: 'om_q9',
    bankId: 'om_mkt',
    question: 'Which three additional Ps expand the traditional 4Ps into the 7Ps Services Marketing Mix?',
    options: [
      'Profit, Purpose, Positioning',
      'People, Process, Physical Evidence',
      'Packaging, Publicity, Partnerships',
      'Planning, Pricing, Performance'
    ],
    answer: 1,
    explanation:
      'Services marketing adds People, Process, and Physical Evidence to Product, Price, Place, and Promotion.'
  },
  {
    id: 'om_q10',
    bankId: 'om_mkt',
    question: 'In the Ansoff Growth Matrix, launching a brand-new product into an unfamiliar new market is called:',
    options: ['Market Penetration', 'Product Development', 'Market Development', 'Diversification'],
    answer: 3,
    explanation:
      'Diversification pairs new products with new markets and carries the highest strategic risk in Ansoff’s matrix.'
  },
  {
    id: 'om_q11',
    bankId: 'om_perf',
    question: 'What are the four perspectives of Kaplan & Norton’s Balanced Scorecard?',
    options: [
      'Profit, Liquidity, Solvency, Gearing',
      'Financial, Customer, Internal Business Processes, Learning & Growth',
      'Core, Middle Line, Technostructure, Support Staff',
      'Product, Price, Place, Promotion'
    ],
    answer: 1,
    explanation:
      'The Balanced Scorecard tracks Financial, Customer, Internal Business Process, and Learning & Growth perspectives.'
  },
  {
    id: 'om_q12',
    bankId: 'om_perf',
    question: 'Fitzgerald & Moon’s Building Block Model for service performance rests on which three pillars?',
    options: [
      'Dimensions, Standards, and Rewards',
      'Inputs, Outputs, and Outcomes',
      'Sort, Shine, and Sustain',
      'Budgeting, Costing, and Auditing'
    ],
    answer: 0,
    explanation:
      'The Building Block Model links 6 Performance Dimensions with 3 Standards (ownership, achievability, equity) and 3 Rewards (clarity, motivation, controllability).'
  }
];

export const STARTER_FLASHCARDS: FlashcardItem[] = [
  {
    id: 'fc_1',
    deck: 'OM Book (ICMA M3)',
    front: "What are Mintzberg's 5 Organizational Parts?",
    back: '1. Operating Core (producers)\n2. Strategic Apex (top executives)\n3. Middle Line (hierarchy)\n4. Technostructure (analysts standardizing work)\n5. Support Staff (indirect services)',
    hint: 'Think from frontline workers up to advisors & staff',
    mastered: false,
    reviewCount: 0
  },
  {
    id: 'fc_2',
    deck: 'OM Book (ICMA M3)',
    front: 'Formula for Overall Equipment Effectiveness (OEE)?',
    back: 'OEE = Availability × Performance × Quality\nCaptures losses from breakdowns, slow cycle speeds, and defective output.',
    hint: 'Three ratios multiplied together',
    mastered: false,
    reviewCount: 0
  },
  {
    id: 'fc_3',
    deck: 'Past Papers (ICMA ML-1)',
    front: 'List 4 drawbacks of a Sales Orientation (Q.5).',
    back: '1. Customer dissatisfaction from aggressive selling\n2. Short-term revenue obsession\n3. Weak R&D & product innovation\n4. Limited customer insight & high sales-staff turnover',
    hint: 'Focuses on pushing existing stock instead of customer needs',
    mastered: false,
    reviewCount: 0
  },
  {
    id: 'fc_4',
    deck: 'Past Papers (ICMA ML-1)',
    front: 'Key disadvantages of the Balanced Scorecard (Q.4)?',
    back: '• Qualitative data is hard to record/process\n• Information overload from too many KPIs\n• Conflict between short-term profit & long-term R&D\n• High setup cost & risk without senior commitment',
    hint: 'Qualitative measurement, overload, and metric conflicts',
    mastered: false,
    reviewCount: 0
  },
  {
    id: 'fc_5',
    deck: 'Past Papers (ICMA ML-1)',
    front: 'What is a Quality Circle & why is it effective (Q.9)?',
    back: 'A small voluntary group of cross-level employees meeting regularly to solve workplace quality issues.\nBenefits: Higher quality & productivity, continuous improvement culture, and frontline ownership.',
    hint: 'Shop-floor staff spot quality problems best',
    mastered: false,
    reviewCount: 0
  },
  {
    id: 'fc_6',
    deck: 'Past Papers (ICMA ML-1)',
    front: '5 Benefits of Material Requirement Planning (MRP) (Q.10)?',
    back: '1. Better customer order fulfillment\n2. Lower stock holding costs\n3. Fast computerized updates on demand shifts\n4. Early bottleneck warnings\n5. Closer supplier coordination',
    hint: 'Inventory, speed, and early warnings',
    mastered: false,
    reviewCount: 0
  }
];

export const ROADMAP_MILESTONES: RoadmapMilestone[] = [
  {
    id: 'ms_1',
    unitLabel: 'Milestone 1 • Units 1–4',
    title: 'Operations Foundations, Mintzberg & TPM/5S',
    description: 'Master PESTEL/ESG, Mintzberg 5 Parts, Manufacturing Systems, 5S & OEE.',
    targetMinutes: 45,
    linkedMaterialId: 'om_study_text_2026',
    linkedTopic: 'Mintzberg 5 Parts'
  },
  {
    id: 'ms_2',
    unitLabel: 'Milestone 2 • Units 5–7',
    title: 'MRP, OPT (Drum-Buffer-Rope) & Quality Systems',
    description: 'Drill MRP benefits, Six Sigma (3.4 DPMO), Juran COQ, Kaizen & Quality Circles.',
    targetMinutes: 45,
    linkedMaterialId: 'om_study_text_2026',
    linkedTopic: 'MRP & OPT'
  },
  {
    id: 'ms_3',
    unitLabel: 'Milestone 3 • Units 8–9',
    title: 'Human Capital, Motivation & HR Learning',
    description: 'Compare Herzberg, Maslow, Handy Shamrock, Pfeffer 7 Practices & Kolb Cycle.',
    targetMinutes: 35,
    linkedMaterialId: 'om_study_text_2026',
    linkedTopic: 'HRM Motivation'
  },
  {
    id: 'ms_4',
    unitLabel: 'Milestone 4 • Units 10–14',
    title: 'Marketing 7Ps, BCG/Ansoff & Balanced Scorecard',
    description: 'Conquer STP, Ansoff/BCG, CSFs vs KPIs, Balanced Scorecard & Performance Pyramid.',
    targetMinutes: 50,
    linkedMaterialId: 'om_study_text_2026',
    linkedTopic: 'Balanced Scorecard'
  },
  {
    id: 'ms_5',
    unitLabel: 'Milestone 5 • Exam Sprint',
    title: 'OM ML-1 Past Papers (Q.1–Q.10) & Timed MCQs',
    description: 'Practice all 10 5-mark suggested solutions and score 80%+ in the MCQ Arena.',
    targetMinutes: 40,
    linkedMaterialId: 'om_past_papers_ml1',
    linkedTopic: 'OM Past Papers Q1-Q10'
  }
];
