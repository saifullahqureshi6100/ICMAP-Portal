import {
  FlashcardItem,
  McqQuestionItem,
  PdfChapterJump,
  RoadmapMilestone,
  StudyMaterialItem,
  SubjectInfo,
  TypingQuestionItem
} from '../types';

export const SUBJECTS: SubjectInfo[] = [
  {
    code: 'OM-A',
    name: 'Part A: Operations, Mintzberg, MRP, OPT & TPM/Quality (Units 1–7)',
    progress: 0,
    weak: ['Mintzberg 5 Parts', 'MRP & OPT', 'Quality Circles & TPM']
  },
  {
    code: 'OM-B',
    name: 'Part B: Human Capital, Motivation, Pfeffer & Kolb (Units 8–9)',
    progress: 0,
    weak: ['HRM & Motivation', 'Pfeffer & Kolb Learning']
  },
  {
    code: 'OM-C',
    name: 'Part C: Marketing Concept, 7Ps, BCG & Ansoff (Units 10–12)',
    progress: 0,
    weak: ['Marketing Concept & STP', '7Ps, BCG & Ansoff']
  },
  {
    code: 'OM-D',
    name: 'Part D: EPM, Balanced Scorecard & Building Block (Units 13–14)',
    progress: 0,
    weak: ['Balanced Scorecard', 'Budgets, CSFs & KPIs']
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
      'Complete 254-page ICMA Pakistan M3 Organizational Management textbook (Units 1–14). Open embedded PDF reader with chapter jumps or select text below for instant Urdu/English AI bullet notes.',
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

Merit-Level Executive Workflow:
1. Morning 8:00 AM (30m): Watch today's recommended lecture clip & preview core unit.
2. Office Break 12:45 PM (30m): Study focused excerpt & clear the 10-question Short Test during Office.
3. Evening 5:30 PM (20m): Tired? Flip today's 5 high-yield Flashcards only.
4. Night 7:00 PM + 8:15 PM: Fresh 1h deep-dive + 60/40 Test of the Day (MCQs + Typing).`
  }
];

export const OM_MCQ_QUESTIONS: McqQuestionItem[] = [
  {
    id: 'om_q1',
    bankId: 'part_a_ops',
    topicTag: 'PESTEL & ESG',
    sourceBenchmark: 'ICMA M3 Unit 1 / ACCA BT',
    question:
      'In PESTEL macro-environmental analysis, carbon-emission limits, waste disposal obligations, and ESG reporting mandates fall primarily under:',
    options: ['Economic', 'Environmental', 'Social', 'Technological'],
    answer: 1,
    explanation: 'Environmental factors cover sustainability, climate risk, carbon legislation, and ESG compliance.'
  },
  {
    id: 'om_q2',
    bankId: 'part_a_ops',
    topicTag: 'Mintzberg 5 Parts',
    sourceBenchmark: 'ICMA Past Paper Q.6',
    question:
      'In Mintzberg’s Effective Organization, which part consists of analysts (like management accountants and HR planners) who standardize work processes and outputs?',
    options: ['Operating Core', 'Strategic Apex', 'Technostructure', 'Support Staff'],
    answer: 2,
    explanation:
      'Mintzberg’s Technostructure standardizes processes, outputs, and skills outside the direct line of managerial authority.'
  },
  {
    id: 'om_q3',
    bankId: 'part_a_ops',
    topicTag: 'Mintzberg 5 Parts',
    sourceBenchmark: 'ACCA BT / CIMA E1',
    question:
      'Which Mintzberg configuration relies heavily on direct supervision from the Strategic Apex with minimal Technostructure and suits entrepreneurial startups?',
    options: ['Machine Bureaucracy', 'Simple Structure', 'Professional Bureaucracy', 'Divisionalized Form'],
    answer: 1,
    explanation: 'Simple Structure concentrates strategic and operational authority in the top founder/Strategic Apex.'
  },
  {
    id: 'om_q4',
    bankId: 'part_a_ops',
    topicTag: 'Production Systems',
    sourceBenchmark: 'ICMA M3 Unit 3',
    question:
      'Constructing custom defense vessels or specialized bridges strictly to individual client blueprints uses which production technique?',
    options: ['Flow Process Production', 'Batch Production', 'Job Production', 'Cellular Assembly'],
    answer: 2,
    explanation: 'Job production handles unique one-off projects built to bespoke customer specifications.'
  },
  {
    id: 'om_q5',
    bankId: 'part_a_ops',
    topicTag: 'Quality Circles & TPM',
    sourceBenchmark: 'ICMA M3 Unit 4',
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
    id: 'om_q6',
    bankId: 'part_a_ops',
    topicTag: 'Quality Circles & TPM',
    sourceBenchmark: 'CIMA P1 / ICMA M3',
    question: 'In TPM numerical evaluation, if Availability = 90%, Performance = 85%, and Quality = 96%, what is OEE?',
    options: ['73.44%', '90.33%', '76.50%', '81.60%'],
    answer: 0,
    explanation: '0.90 × 0.85 × 0.96 = 0.7344 = 73.44%, below the 85% world-class OEE benchmark.'
  },
  {
    id: 'om_q7',
    bankId: 'part_a_ops',
    topicTag: 'Quality Circles & TPM',
    sourceBenchmark: 'ICMA M3 Unit 4',
    question: 'In Japanese 5S methodology, "Seiso" (Shine) specifically requires:',
    options: [
      'Sorting obsolete tools out of the factory',
      'Cleaning equipment thoroughly to detect leaks or abnormalities early',
      'Creating signage for aisles',
      'Holding annual HR appraisals'
    ],
    answer: 1,
    explanation: 'Seiso combines workspace cleaning with proactive preventive inspection.'
  },
  {
    id: 'om_q8',
    bankId: 'part_a_ops',
    topicTag: 'MRP & OPT',
    sourceBenchmark: 'ICMA Past Paper Q.10',
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
    id: 'om_q9',
    bankId: 'part_a_ops',
    topicTag: 'MRP & OPT',
    sourceBenchmark: 'ICMA Past Paper Q.10 / CIMA E1',
    question: 'Which is a core financial/operational benefit of Material Requirement Planning (MRP)?',
    options: [
      'Increasing excess safety stock levels across all warehouses',
      'Reducing stock holding costs and giving early warning of supply bottlenecks',
      'Replacing strategic budgets entirely',
      'Preventing dynamic schedule updates when demand changes'
    ],
    answer: 1,
    explanation: 'MRP cuts inventory holding cost, speeds order fulfillment, and alerts managers early to bottleneck parts.'
  },
  {
    id: 'om_q10',
    bankId: 'part_a_ops',
    topicTag: 'Capacity & Queuing',
    sourceBenchmark: 'ICMA M3 Unit 6 / ACCA PM',
    question: 'Practical (Effective) Capacity equals Design Capacity adjusted for:',
    options: [
      'Stock market volatility and share buybacks',
      'Planned preventive maintenance, machine setups, and shift changeovers',
      'Advertising budget increases',
      'Future 10-year expansion dreams'
    ],
    answer: 1,
    explanation: 'Effective capacity deducts realistic scheduled allowances like maintenance, setups, and shift constraints.'
  },
  {
    id: 'om_q11',
    bankId: 'part_a_ops',
    topicTag: 'Quality Circles & TPM',
    sourceBenchmark: 'ICMA M3 Unit 7',
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
    id: 'om_q12',
    bankId: 'part_a_ops',
    topicTag: 'Quality Circles & TPM',
    sourceBenchmark: 'CIMA P1 / Juran COQ',
    question: 'In Juran’s Cost of Quality (COQ) model, inspection, testing, and quality audit costs fall under:',
    options: ['Internal Failure Costs', 'External Failure Costs', 'Appraisal Costs', 'Prevention Costs'],
    answer: 2,
    explanation: 'Appraisal costs inspect and test output to detect defects before delivery.'
  },
  {
    id: 'om_q13',
    bankId: 'part_a_ops',
    topicTag: 'Quality Circles & TPM',
    sourceBenchmark: 'ICMA Past Paper Q.9',
    question: 'Why are Quality Circles effective in reducing operational defect rates?',
    options: [
      'External auditors impose penalties during surprise visits',
      'Voluntary cross-level shop-floor teams understand everyday quality bottlenecks best',
      'Only board directors are allowed to join them',
      'They eliminate all employee training programs'
    ],
    answer: 1,
    explanation: 'Quality Circles tap frontline employee insight, build unity, and sustain continuous improvement.'
  },
  {
    id: 'om_q14',
    bankId: 'part_b_hrm',
    topicTag: 'HRM & Motivation',
    sourceBenchmark: 'ICMA Past Paper Q.2 / ACCA BT',
    question: 'Which set lists external macro factors affecting an organization’s HRM process?',
    options: [
      'Span of control, office layout, intranet speed',
      'Economy, labor unions, legal environment, and demographic trends',
      'Six Sigma, Kanban, and OEE ratios',
      'BCG Cash Cows and Ansoff Diversification'
    ],
    answer: 1,
    explanation: 'ICMA M3 Past Paper Q.2 highlights Economy, Labor Unions, Legal Environment, and Demographics.'
  },
  {
    id: 'om_q15',
    bankId: 'part_b_hrm',
    topicTag: 'HRM & Motivation',
    sourceBenchmark: 'ACCA BT / Herzberg',
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
    id: 'om_q16',
    bankId: 'part_b_hrm',
    topicTag: 'HRM & Motivation',
    sourceBenchmark: 'ICMA M3 Unit 8 / CIMA E1',
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
    id: 'om_q17',
    bankId: 'part_b_hrm',
    topicTag: 'HRM & Motivation',
    sourceBenchmark: 'ICMA Past Paper Q.8',
    question: 'When an interviewer lets one standout trait (good or bad) color their entire rating of a candidate, it is called:',
    options: ['Logical Error', 'Contagious Bias', 'Halo Effect', 'Predictive Validity'],
    answer: 2,
    explanation: 'The Halo Effect allows a single dominant impression to distort the overall interview assessment.'
  },
  {
    id: 'om_q18',
    bankId: 'part_b_hrm',
    topicTag: 'Pfeffer & Kolb Learning',
    sourceBenchmark: 'ICMA M3 Unit 9',
    question: 'Which practice is part of Jeffrey Pfeffer’s 7 Practices of Successful Organizations?',
    options: [
      'Restricting financial performance data to board directors only',
      'Self-managed teams, selective hiring, and open sharing of financial information',
      'Zero formal training budget and strict rank privileges',
      'Annual layoffs to keep wages low'
    ],
    answer: 1,
    explanation: 'Pfeffer’s 7 practices emphasize security, selective hiring, teams, high contingent pay, training, lower status gaps, and transparency.'
  },
  {
    id: 'om_q19',
    bankId: 'part_b_hrm',
    topicTag: 'Pfeffer & Kolb Learning',
    sourceBenchmark: 'ICMA M3 Unit 9 / ACCA BT',
    question: 'What is the correct sequence in Kolb’s Experiential Learning Cycle?',
    options: [
      'Abstract Conceptualization → Active Experimentation → Concrete Experience → Reflective Observation',
      'Concrete Experience → Reflective Observation → Abstract Conceptualization → Active Experimentation',
      'Reflective Observation → Concrete Experience → Abstract Conceptualization → Active Experimentation',
      'Active Experimentation → Concrete Experience → Reflective Observation → Abstract Conceptualization'
    ],
    answer: 1,
    explanation: 'Kolb moves from Doing (Concrete Experience) → Reviewing (Reflective) → Concluding (Abstract) → Testing (Active Experimentation).'
  },
  {
    id: 'om_q20',
    bankId: 'part_b_hrm',
    topicTag: 'HRM & Motivation',
    sourceBenchmark: 'ICMA Past Paper Q.3',
    question: 'A firm hires an external recruitment agency rather than advertising a CFO replacement internally primarily to:',
    options: [
      'Ensure every competitor sees its expansion plans immediately',
      'Preserve confidentiality for senior headhunting and avoid signaling high turnover',
      'Increase screening costs deliberately',
      'Avoid using structured candidate databases'
    ],
    answer: 1,
    explanation: 'Agencies safeguard executive confidentiality, prevent competitor alerts, and manage high applicant volumes efficiently.'
  },
  {
    id: 'om_q21',
    bankId: 'part_c_mkt',
    topicTag: 'Marketing Concept & STP',
    sourceBenchmark: 'ICMA Past Paper Q.1',
    question: 'Unlike Sales Orientation, the Marketing Concept orientation starts by:',
    options: [
      'Maximizing factory utilization regardless of buyer tastes',
      'Determining target market needs, wants, and values and delivering superior satisfaction',
      'Using aggressive outbound cold-calling to liquidate leftover stock',
      'Cutting R&D spend to maximize short-term cashflow'
    ],
    answer: 1,
    explanation: 'The Marketing Concept centers on discovering customer needs and outperforming rivals in delivering value.'
  },
  {
    id: 'om_q22',
    bankId: 'part_c_mkt',
    topicTag: 'Marketing Concept & STP',
    sourceBenchmark: 'ICMA Past Paper Q.5',
    question: 'Which risk commonly afflicts firms relying heavily on a Sales Orientation (e.g., aggressive telemarketing or MLM)?',
    options: [
      'Excessive long-term R&D over-investment',
      'Customer dissatisfaction, short-termism, weak innovation, and high sales-staff turnover',
      'Over-reliance on deep qualitative ethnography',
      'Complete elimination of distribution margins'
    ],
    answer: 1,
    explanation: 'Sales orientation pushes existing goods hard, breeding buyer regret, neglected R&D, and high rep attrition.'
  },
  {
    id: 'om_q23',
    bankId: 'part_c_mkt',
    topicTag: '7Ps, BCG & Ansoff',
    sourceBenchmark: 'ICMA M3 Unit 11 / CIMA E1',
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
    id: 'om_q24',
    bankId: 'part_c_mkt',
    topicTag: '7Ps, BCG & Ansoff',
    sourceBenchmark: 'ACCA APM / Ansoff',
    question: 'In the Ansoff Growth Matrix, launching a brand-new product into an unfamiliar new market is called:',
    options: ['Market Penetration', 'Product Development', 'Market Development', 'Diversification'],
    answer: 3,
    explanation:
      'Diversification pairs new products with new markets and carries the highest strategic risk in Ansoff’s matrix.'
  },
  {
    id: 'om_q25',
    bankId: 'part_c_mkt',
    topicTag: '7Ps, BCG & Ansoff',
    sourceBenchmark: 'ICMA M3 Unit 11 / ICAEW B&S',
    question: 'In the BCG Growth-Share Matrix, a dominant market-share product in a slow-growing mature industry is classified as a:',
    options: ['Star', 'Question Mark', 'Cash Cow', 'Dog'],
    answer: 2,
    explanation: 'Cash Cows generate surplus cash that finances Question Marks and Stars needing heavy investment.'
  },
  {
    id: 'om_q26',
    bankId: 'part_c_mkt',
    topicTag: 'Consumer Behavior & CSR',
    sourceBenchmark: 'ICMA M3 Unit 12',
    question: 'In organizational buying (DMU), the specialist finance officer who sets formal vendor specs or controls budget approval acts primarily as:',
    options: ['Consumer influencer only', 'Gatekeeper / Buyer / Approver within the Decision Making Unit', 'End Retail Shopper', 'Passive Bystander'],
    answer: 1,
    explanation: 'B2B DMUs split roles into Users, Influencers, Buyers, Deciders/Approvers, and Gatekeepers.'
  },
  {
    id: 'om_q27',
    bankId: 'part_d_epm',
    topicTag: 'Budgets, CSFs & KPIs',
    sourceBenchmark: 'ICMA Past Paper Q.7 / ACCA PM',
    question: 'How do CSFs (Critical Success Factors) and KPIs relate in Enterprise Performance Management?',
    options: [
      'CSFs are vital strategic performance areas; KPIs quantify progress against them',
      'KPIs are strictly qualitative while CSFs are always cash balances',
      'CSFs replace annual budgets entirely',
      'They are mutually exclusive frameworks'
    ],
    answer: 0,
    explanation: 'Rockart CSFs define what the firm must excel at; KPIs attach measurable numbers and targets.'
  },
  {
    id: 'om_q28',
    bankId: 'part_d_epm',
    topicTag: 'Balanced Scorecard',
    sourceBenchmark: 'ICMA Past Paper Q.4 / CIMA P2',
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
    id: 'om_q29',
    bankId: 'part_d_epm',
    topicTag: 'Balanced Scorecard',
    sourceBenchmark: 'ICMA Past Paper Q.4',
    question: 'Which is a key disadvantage of the Balanced Scorecard tested in ICMA OM ML-1?',
    options: [
      'It ignores customer metrics completely',
      'Information overload, difficulty processing qualitative data, and potential conflict between metrics',
      'Zero setup or IT tracking cost',
      'It guarantees senior leadership commitment automatically'
    ],
    answer: 1,
    explanation: 'Too many KPIs, hard-to-record qualitative data, and short-term vs R&D conflicts can derail BSC without senior backing.'
  },
  {
    id: 'om_q30',
    bankId: 'part_d_epm',
    topicTag: 'Building Block & Pyramid',
    sourceBenchmark: 'ACCA PM / ICMA M3 Unit 14',
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

export const TYPING_QUESTIONS: TypingQuestionItem[] = [
  {
    id: 'typ_1',
    topicTag: 'Balanced Scorecard',
    marks: 5,
    sourceBenchmark: 'ICMA ML-1 Past Paper Q.4',
    question: 'Explain five practical disadvantages an organization may face when implementing the Balanced Scorecard.',
    keywords: ['qualitative', 'overload', 'conflict', 'misalign', 'cost', 'commitment', 'short-term'],
    modelAnswer:
      '1. Difficult to record and process qualitative (non-financial) data.\n2. Information overload from tracking too many measures.\n3. Conflict between measures (e.g., cutting R&D boosts short-term profit but harms innovation).\n4. Chosen KPIs may misalign with strategy if poorly linked.\n5. High setup/measurement cost and failure risk if senior management lacks commitment or still rewards narrow financial targets only.'
  },
  {
    id: 'typ_2',
    topicTag: 'Mintzberg 5 Parts',
    marks: 5,
    sourceBenchmark: 'ICMA ML-1 Past Paper Q.6',
    question: 'Identify and explain the five core parts of Mintzberg’s Effective Organization with one corporate example each.',
    keywords: ['operating core', 'strategic apex', 'middle line', 'technostructure', 'support staff'],
    modelAnswer:
      '1. Strategic Apex: Top executives setting strategy (e.g., CEO & Board).\n2. Middle Line: Managers linking apex to operations (e.g., Regional Managers).\n3. Operating Core: Frontline workers producing goods/services (e.g., assembly technicians, bank tellers).\n4. Technostructure: Analysts standardizing work/outputs/skills outside line authority (e.g., Management Accountants, HR planners).\n5. Support Staff: Indirect services outside workflow (e.g., Legal counsel, PR, security).'
  },
  {
    id: 'typ_3',
    topicTag: 'MRP & OPT',
    marks: 5,
    sourceBenchmark: 'ICMA ML-1 Past Paper Q.10 / CIMA E1',
    question: 'Outline five operational and financial benefits of implementing Material Requirement Planning (MRP). Also contrast Drum-Buffer-Rope in OPT briefly.',
    keywords: ['customer orders', 'inventory', 'holding', 'schedule', 'bottleneck', 'supplier'],
    modelAnswer:
      'MRP Benefits:\n1. Improved ability to meet customer delivery deadlines.\n2. Reduced inventory holding and working-capital costs.\n3. Fast computerized rescheduling when customer demand shifts.\n4. Early warning of purchasing or production bottlenecks.\n5. Closer long-term supplier coordination.\nOPT Contrast: OPT anchors schedules around bottlenecks using Drum (pace), Buffer (protective stock), and Rope (material release timing).'
  },
  {
    id: 'typ_4',
    topicTag: 'Quality Circles & TPM',
    marks: 5,
    sourceBenchmark: 'ICMA ML-1 Past Paper Q.9',
    question: 'Define a Quality Circle and state four advantages it creates for manufacturing or finance service operations.',
    keywords: ['voluntary', 'regular', 'quality', 'productivity', 'shop-floor', 'continuous', 'unity'],
    modelAnswer:
      'Definition: A small voluntary group of mixed-skill employees from across levels meeting regularly to identify and solve real quality problems.\nAdvantages:\n1. Higher product/service quality, customer satisfaction, and productivity.\n2. Frontline operational staff spot root shop-floor bottlenecks managers miss.\n3. Builds a Kaizen continuous improvement culture.\n4. Strengthens morale, empowerment, and organizational unity.'
  },
  {
    id: 'typ_5',
    topicTag: 'HRM & Motivation',
    marks: 5,
    sourceBenchmark: 'ICMA ML-1 Past Paper Q.8 / ACCA BT',
    question: 'Explain four judgment errors that reduce the predictive validity of job selection interviews.',
    keywords: ['halo', 'contagious', 'stereotyping', 'logical', 'validity', 'traits'],
    modelAnswer:
      'Job interviews often suffer low interviewer agreement and poor predictive validity due to:\n1. Halo Effect: Allowing one standout positive or negative trait to distort the overall score.\n2. Contagious Bias: Giving unconscious leading cues or comparing against the prior candidate.\n3. Stereotyping: Prejudging candidates by dress, accent, or voice.\n4. Logical Error / Trait Misjudgment: Assuming unrelated traits correlate or misreading honesty/motivation.'
  },
  {
    id: 'typ_6',
    topicTag: 'Budgets, CSFs & KPIs',
    marks: 5,
    sourceBenchmark: 'ICMA ML-1 Past Paper Q.7 / ACCA PM',
    question: 'How does an effective budgetary control system help management maximize profit and accountability?',
    keywords: ['cost', 'variance', 'efficiency', 'coordination', 'incentives', 'responsibility'],
    modelAnswer:
      '1. Controls wasteful expenditure and boosts cost efficiency.\n2. Assigns responsibility by measuring departmental and cost-center performance.\n3. Highlights unfavorable variances early so corrective action is taken.\n4. Aligns inter-departmental coordination between sales, procurement, and production.\n5. Supports goal-congruent performance incentives linked to profit targets.'
  },
  {
    id: 'typ_7',
    topicTag: 'Marketing Concept & STP',
    marks: 5,
    sourceBenchmark: 'ICMA ML-1 Past Paper Q.1 & Q.5',
    question: 'Define the Marketing Concept and explain three commercial risks faced by organizations still using a Sales Orientation.',
    keywords: ['needs', 'wants', 'satisfaction', 'short-term', 'dissatisfaction', 'innovation', 'turnover'],
    modelAnswer:
      'Marketing Concept: Determining target market needs, wants, and values and delivering superior customer satisfaction more effectively than rivals.\nRisks of Sales Orientation:\n1. Customer dissatisfaction from high-pressure selling.\n2. Short-term revenue obsession damaging long-term brand loyalty.\n3. Weak R&D innovation and poor customer insight, plus high sales-staff turnover.'
  },
  {
    id: 'typ_8',
    topicTag: 'Building Block & Pyramid',
    marks: 5,
    sourceBenchmark: 'ACCA PM / ICMA M3 Unit 14',
    question: 'Describe the three pillars of Fitzgerald & Moon’s Building Block Model for a service firm (e.g., commercial bank).',
    keywords: ['dimensions', 'standards', 'rewards', 'ownership', 'achievability', 'controllability'],
    modelAnswer:
      '1. Dimensions (6 KPI areas): Two results (Financial, Competitiveness) and four determinants (Quality, Flexibility, Resource Utilization, Innovation).\n2. Standards: Targets must possess Ownership, Achievability, and Equity (fairness).\n3. Rewards: Incentives must offer Clarity, Motivation, and Controllability (staff evaluated only on factors they control).'
  },
  {
    id: 'typ_9',
    topicTag: 'Pfeffer & Kolb Learning',
    marks: 5,
    sourceBenchmark: 'ICMA M3 Unit 9 / CIMA E1',
    question: 'List Jeffrey Pfeffer’s 7 Practices of Successful Organizations and explain how transparency supports performance.',
    keywords: ['security', 'selective', 'teams', 'contingent', 'training', 'status', 'financial'],
    modelAnswer:
      'Pfeffer’s 7 Practices: (1) Employment security, (2) Selective hiring, (3) Self-managed teams & decentralization, (4) Comparatively high pay contingent on performance, (5) Extensive training, (6) Reduced status distinctions, and (7) Extensive sharing of financial and performance information.\nSharing financial info builds trust so staff understand unit economics and reduce waste.'
  },
  {
    id: 'typ_10',
    topicTag: '7Ps, BCG & Ansoff',
    marks: 5,
    sourceBenchmark: 'ICMA M3 Unit 11 / ICAEW',
    question: 'Using Ansoff’s Growth Matrix, distinguish Market Penetration from Diversification and explain how finance evaluates risk across both.',
    keywords: ['existing', 'new', 'market', 'product', 'penetration', 'diversification', 'risk'],
    modelAnswer:
      'Market Penetration sells existing products into existing markets (e.g., loyalty discounts) with lowest capital risk.\nDiversification launches brand-new products into unfamiliar new markets (related or conglomerate), carrying the highest operational, branding, and capital-budgeting risk.'
  }
];

export const STARTER_FLASHCARDS: FlashcardItem[] = [
  {
    id: 'fc_1',
    deck: 'OM Book (ICMA M3)',
    topicTag: 'Mintzberg 5 Parts',
    dayOffsetTag: -1,
    front: "What are Mintzberg's 5 Organizational Parts?",
    back: '1. Operating Core (producers)\n2. Strategic Apex (top executives)\n3. Middle Line (hierarchy)\n4. Technostructure (analysts standardizing work)\n5. Support Staff (indirect services)',
    hint: 'Think from frontline workers up to advisors & staff',
    mastered: false,
    reviewCount: 0
  },
  {
    id: 'fc_2',
    deck: 'OM Book (ICMA M3)',
    topicTag: 'Quality Circles & TPM',
    dayOffsetTag: -2,
    front: 'Formula for Overall Equipment Effectiveness (OEE)?',
    back: 'OEE = Availability × Performance × Quality\nCaptures losses from breakdowns, slow cycle speeds, and defective output (world-class = 85%+).',
    hint: 'Three ratios multiplied together',
    mastered: false,
    reviewCount: 0
  },
  {
    id: 'fc_3',
    deck: 'Past Papers (ICMA ML-1)',
    topicTag: 'Marketing Concept & STP',
    dayOffsetTag: 0,
    front: 'List 4 drawbacks of a Sales Orientation (Past Paper Q.5).',
    back: '1. Customer dissatisfaction from aggressive selling\n2. Short-term revenue obsession\n3. Weak R&D & product innovation\n4. Limited customer insight & high sales-staff turnover',
    hint: 'Focuses on pushing existing stock instead of customer needs',
    mastered: false,
    reviewCount: 0
  },
  {
    id: 'fc_4',
    deck: 'Past Papers (ICMA ML-1)',
    topicTag: 'Balanced Scorecard',
    dayOffsetTag: 0,
    front: 'Key disadvantages of the Balanced Scorecard (Past Paper Q.4)?',
    back: '• Qualitative data is hard to record/process\n• Information overload from too many KPIs\n• Conflict between short-term profit & long-term R&D\n• High setup cost & risk without senior commitment',
    hint: 'Qualitative measurement, overload, and metric conflicts',
    mastered: false,
    reviewCount: 0
  },
  {
    id: 'fc_5',
    deck: 'Past Papers (ICMA ML-1)',
    topicTag: 'Quality Circles & TPM',
    dayOffsetTag: -1,
    front: 'What is a Quality Circle & why is it effective (Past Paper Q.9)?',
    back: 'A small voluntary group of cross-level employees meeting regularly to solve workplace quality issues.\nBenefits: Higher quality & productivity, continuous improvement culture, and frontline ownership.',
    hint: 'Shop-floor staff spot quality problems best',
    mastered: false,
    reviewCount: 0
  },
  {
    id: 'fc_6',
    deck: 'Past Papers (ICMA ML-1)',
    topicTag: 'MRP & OPT',
    dayOffsetTag: -2,
    front: '5 Benefits of Material Requirement Planning (MRP) (Past Paper Q.10)?',
    back: '1. Better customer order fulfillment\n2. Lower stock holding costs\n3. Fast computerized updates on demand shifts\n4. Early bottleneck warnings\n5. Closer supplier coordination',
    hint: 'Inventory, speed, and early warnings',
    mastered: false,
    reviewCount: 0
  },
  {
    id: 'fc_7',
    deck: 'OM Book (ICMA M3)',
    topicTag: 'MRP & OPT',
    dayOffsetTag: 1,
    front: 'Explain OPT Drum-Buffer-Rope scheduling (Upcoming).',
    back: '• Drum: Bottleneck sets total pace\n• Buffer: Stock/time protection before the bottleneck\n• Rope: Signal tying raw-material release to bottleneck consumption',
    hint: 'Theory of Constraints factory rhythm',
    mastered: false,
    reviewCount: 0
  },
  {
    id: 'fc_8',
    deck: 'OM Book (ICMA M3)',
    topicTag: 'Building Block & Pyramid',
    dayOffsetTag: 1,
    front: 'Fitzgerald & Moon Building Block Model — 3 Pillars?',
    back: '1. Dimensions (2 results + 4 determinants)\n2. Standards (Ownership, Achievability, Equity)\n3. Rewards (Clarity, Motivation, Controllability)',
    hint: 'Designed specifically for service-sector control',
    mastered: false,
    reviewCount: 0
  },
  {
    id: 'fc_9',
    deck: 'OM Book (ICMA M3)',
    topicTag: 'HRM & Motivation',
    dayOffsetTag: -1,
    front: '3 Leaves of Charles Handy’s Shamrock Organization?',
    back: '1. Professional Core (essential specialists)\n2. Contractual Fringe (outsourced services)\n3. Flexible Labor Force (part-time/temp)',
    hint: 'Core + Outsourced + Flexible',
    mastered: false,
    reviewCount: 0
  },
  {
    id: 'fc_10',
    deck: 'OM Book (ICMA M3)',
    topicTag: '7Ps, BCG & Ansoff',
    dayOffsetTag: 1,
    front: 'Name the 4 quadrants of BCG Growth-Share & Ansoff Matrices.',
    back: 'BCG: Stars, Cash Cows, Question Marks, Dogs\nAnsoff: Market Penetration, Product Development, Market Development, Diversification',
    hint: 'Portfolio cash generation vs Growth risk vectors',
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
    linkedTopic: 'HRM & Motivation'
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
    title: 'OM ML-1 Past Papers (Q.1–Q.10) & Full Mocks',
    description: 'Practice all 10 5-mark suggested solutions and score 85%+ across 60/40 Mocks.',
    targetMinutes: 60,
    linkedMaterialId: 'om_past_papers_ml1',
    linkedTopic: 'Balanced Scorecard'
  }
];
