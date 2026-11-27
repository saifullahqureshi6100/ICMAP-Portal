package com.example.data.syllabus

data class GlossaryTerm(
    val id: String,
    val term: String,
    val partCode: String,
    val definition: String,
    val romanUrduExplanation: String,
    val pakistaniExample: String,
    val examTip: String
)

object OmGlossaryCatalog {
    val terms = listOf(
        GlossaryTerm(
            id = "g_mrp",
            term = "MRP (Material Requirements Planning)",
            partCode = "PART_A",
            definition = "A computer-based production planning and inventory control system designed to manage manufacturing processes.",
            romanUrduExplanation = "Raw material ki demand calculate karna taake production time pe ho aur unnecessary stock na roke.",
            pakistaniExample = "Packages Limited ya Millat Tractors mein steel sheets aur parts ki exact requirement calculate karna.",
            examTip = "Crucial for Part A Operations Management (35%). Focus on Master Production Schedule (MPS) inputs."
        ),
        GlossaryTerm(
            id = "g_erp",
            term = "ERP (Enterprise Resource Planning)",
            partCode = "PART_A",
            definition = "Integrated management of main business processes, often in real-time and mediated by software and technology.",
            romanUrduExplanation = "Company ke tamam departments (finance, HR, production, sales) ko aik software mein connect karna.",
            pakistaniExample = "Lucky Cement ya Engro Corp jo SAP ERP use karti hain.",
            examTip = "Compare ERP with MRP in exams: ERP covers whole enterprise; MRP focuses on manufacturing/materials."
        ),
        GlossaryTerm(
            id = "g_tqm",
            term = "TQM (Total Quality Management)",
            partCode = "PART_A",
            definition = "An organization-wide effort to instill a permanent climate where employees continuously improve their ability to provide on-demand products.",
            romanUrduExplanation = "Har employee aur process mein quality improve karna, sirf quality department ki zimmedari nahi.",
            pakistaniExample = "Nestle Pakistan mein har batch ki quality testing.",
            examTip = "Remember Deming's 14 points and continuous improvement (Kaizen)."
        ),
        GlossaryTerm(
            id = "g_jit",
            term = "JIT (Just-In-Time Manufacturing)",
            partCode = "PART_A",
            definition = "A workflow methodology aimed at reducing flow times within production systems as well as response times from suppliers.",
            romanUrduExplanation = "Stock zero rakhna; material tab mangwana jab customer order de ya production start ho.",
            pakistaniExample = "Toyota Indus Motor Karachi mein assembly line ke liye parts ki daily supply.",
            examTip = "Mention Kanban cards and waste reduction (Muda)."
        ),
        GlossaryTerm(
            id = "g_bsc",
            term = "Balanced Scorecard (BSC)",
            partCode = "PART_D",
            definition = "A strategy performance management tool that can be used by managers to keep track of staff execution of activities.",
            romanUrduExplanation = "Sirf financial performance nahi, balki Customer, Internal Business, aur Learning & Growth ko bhi track karna.",
            pakistaniExample = "MCB Bank performance review across financial targets and customer satisfaction.",
            examTip = "Extremely high yield for Part D Performance Management (20%). Draw all 4 perspectives."
        ),
        GlossaryTerm(
            id = "g_eoq",
            term = "EOQ (Economic Order Quantity)",
            partCode = "PART_A",
            definition = "The ideal order quantity a company should purchase to minimize inventory costs such as holding costs, shortage costs, and order costs.",
            romanUrduExplanation = "Woh order size jahan ordering cost aur holding cost ka total minimum ho.",
            pakistaniExample = "K-Electric spare parts ordering calculations.",
            examTip = "Be ready to write the square root formula in numerical questions."
        ),
        GlossaryTerm(
            id = "g_kaizen",
            term = "Kaizen",
            partCode = "PART_A",
            definition = "Japanese business philosophy of continuous improvement of working practices and personal efficiency.",
            romanUrduExplanation = "Choti choti lagataar improvements karna har roz.",
            pakistaniExample = "Pak Suzuki production floors par workers ki daily suggestion boxes.",
            examTip = "Contrast with breakthrough innovation."
        ),
        GlossaryTerm(
            id = "g_swot",
            term = "SWOT Analysis",
            partCode = "PART_C",
            definition = "A strategic planning technique used to help a person or organization identify strengths, weaknesses, opportunities, and threats.",
            romanUrduExplanation = "Apni taqat (Strengths), kamzori (Weaknesses), mauqay (Opportunities), aur khatrat (Threats) ka jaiza lena.",
            pakistaniExample = "Shan Foods analyzing global market entry.",
            examTip = "Internal factors = Strengths & Weaknesses; External = Opportunities & Threats."
        ),
        GlossaryTerm(
            id = "g_pestel",
            term = "PESTEL Framework",
            partCode = "PART_C",
            definition = "A strategic framework used by organizations to assess macro-environmental factors: Political, Economic, Social, Technological, Environmental, Legal.",
            romanUrduExplanation = "Bairooni mahol (Siyasat, Economy, Society, Technology) ka analysis.",
            pakistaniExample = "Telenor Pakistan evaluating new telecom regulations.",
            examTip = "Used in strategic marketing and corporate management questions."
        ),
        GlossaryTerm(
            id = "g_kanban",
            term = "Kanban",
            partCode = "PART_A",
            definition = "A scheduling system for lean and just-in-time manufacturing that determines what to produce, when to produce it, and how much to produce.",
            romanUrduExplanation = "Visual card system jo batata hai ke agla part kab ban banana hai.",
            pakistaniExample = "Automobile parts bins with signal cards.",
            examTip = "Key tool for pull production systems."
        ),
        GlossaryTerm(
            id = "g_six_sigma",
            term = "Six Sigma",
            partCode = "PART_A",
            definition = "A set of techniques and tools for process improvement aiming for maximum 3.4 defects per million opportunities.",
            romanUrduExplanation = "Ghalti ki gunjaish na hone ke barabar karna (3.4 defects per million).",
            pakistaniExample = "Engro Fertilizers chemical purity processes.",
            examTip = "DMAIC cycle (Define, Measure, Analyze, Improve, Control) is essential."
        ),
        GlossaryTerm(
            id = "g_value_chain",
            term = "Value Chain Analysis",
            partCode = "PART_C",
            definition = "A set of activities that an organization carries out to create value for its customers (Primary vs Support activities).",
            romanUrduExplanation = "Primary activities (logistics, marketing) aur Support activities (HR, procurement) ka analysis jo customer value banaye.",
            pakistaniExample = "Lucky Cement quarrying to final delivered cement bag.",
            examTip = "Distinguish Porter's primary vs support activities clearly."
        )
    )
}
