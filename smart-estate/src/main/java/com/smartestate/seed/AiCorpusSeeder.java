package com.smartestate.seed;

import com.smartestate.model.BylawDoc;
import com.smartestate.model.Language;
import com.smartestate.repo.BylawDocRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Seeds the bilingual bylaw / FAQ corpus that the RAG chatbot retrieves over (FR-8.1).
 *
 * <p>Every document carries the SAME content in English and 繁體中文, not an English rule with a
 * Chinese footnote. This matters for retrieval, not just presentation: a Chinese question can only
 * match Chinese text, so a corpus that is 98% English answers Chinese questions badly however good
 * the ranking is. {@code RagService} chunks each language separately and detects the language of
 * each chunk, so a query is answered from the passage written in the language it was asked in.
 */
@Component
@Order(2)
public class AiCorpusSeeder implements CommandLineRunner {

    private final BylawDocRepository repo;

    public AiCorpusSeeder(BylawDocRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(String... args) {
        if (repo.count() > 0) return;

        // ---- Core bylaws ----------------------------------------------------------------------

        doc("BL-FEES", "Management fee payment and late charges / 管理費繳付及逾期附加費", "Fees",
            "Management fees are payable monthly in advance and fall due on the first day of each month. "
            + "Payment may be made by autopay, at the management office, or by cheque. If a fee remains unpaid "
            + "for more than 30 days it becomes overdue and a late charge of 5% is added to the outstanding balance. "
            + "Persistent non-payment may be referred to the Owners' Corporation for recovery. ",
            "管理費須每月預繳，並於每月一日到期。住戶可透過自動轉賬、親臨管理處或以支票繳付。"
            + "若管理費逾期超過三十天仍未繳付，即屬欠繳，並會就未繳結餘徵收百分之五的逾期附加費。"
            + "持續欠繳者，管理處會將個案轉交業主立案法團循法律途徑追討，所引致的訟費及利息由業主承擔。");

        doc("BL-NOISE", "Noise and quiet hours / 噪音及安寧時段", "Noise",
            "Residents must not cause noise that disturbs neighbours. Quiet hours are from 23:00 to 08:00, "
            + "during which no drilling, loud music, mahjong, or other noisy activities are permitted. "
            + "Repeated noise complaints may be reported to the management office and, where necessary, the police. ",
            "住戶不得發出滋擾鄰居的噪音。安寧時段為晚上十一時至翌日早上八時，期間不得進行鑽牆、鑽地、"
            + "播放響亮音樂、打麻將或其他擾人活動。若噪音滋擾持續，住戶可向管理處投訴；"
            + "深夜時段的噪音亦可直接報警處理，警方可根據《噪音管制條例》採取行動。");

        doc("BL-RENO", "Renovation rules, permitted hours and permits / 裝修規則、施工時間及許可證", "Renovation",
            "Renovation works require prior written approval from the management office and a refundable deposit. "
            + "Works producing noise or dust are only allowed Monday to Saturday from 09:00 to 18:00, and are not "
            + "permitted on Sundays or public holidays. Debris must be removed daily and the lift protected. "
            + "Structural alterations affecting load-bearing walls are strictly prohibited. ",
            "裝修工程須事先向管理處申請書面批准並繳付可退還按金。產生噪音或塵埃的工程只可於星期一至星期六"
            + "上午九時至下午六時進行，星期日及公眾假期一律不得進行。裝修廢料必須每日清理，"
            + "升降機須加設保護墊。任何影響主力牆的結構改動均嚴格禁止。");

        doc("BL-PET", "Pet policy / 寵物守則", "Pets",
            "Small domestic pets such as cats and small dogs are permitted provided they do not cause a nuisance. "
            + "Dogs must be leashed in all common areas and owners must clean up after them immediately. "
            + "Pets are not allowed in the lift lobby seating area or the children's playground. "
            + "Aggressive breeds and any animal causing repeated complaints may be ordered removed. ",
            "住戶可飼養貓隻及小型犬隻等小型家庭寵物，惟不得造成滋擾。狗隻在所有公用地方必須繫上狗繩，"
            + "主人須即時清理寵物糞便。寵物不得進入大堂休憩座位範圍及兒童遊樂場。"
            + "烈性犬種，以及任何屢遭投訴的動物，管理處可要求飼主遷離。導盲犬及輔助犬不受此限。");

        doc("BL-FIRE", "Fire safety — keep escape routes clear / 消防安全：保持走火通道暢通", "Fire safety",
            "Corridors, staircases, and lift lobbies are designated means of escape and must be kept completely clear "
            + "at all times. Do not store bicycles, shoe cabinets, plants, or any belongings in these areas, as they "
            + "obstruct evacuation and are a fire hazard. Fire doors must never be propped open. In case of fire, use "
            + "the staircase, never the lift, and proceed to the assembly area at the basketball court. ",
            "走廊、樓梯及升降機大堂均屬指定走火通道，必須時刻保持完全暢通。切勿在該等地方擺放單車、鞋櫃、"
            + "盆栽或任何雜物，以免阻礙疏散並構成火警危險。防煙門必須保持關閉，嚴禁以任何物件撐開。"
            + "發生火警時，請使用樓梯逃生，切勿使用升降機，並前往籃球場的集合點等候指示。");

        doc("BL-PARK", "Car park rules / 停車場守則", "Car park",
            "Parking spaces are allocated to registered tenants only and may not be sub-let. Vehicles must display a "
            + "valid parking permit. The speed limit within the car park is 10 km/h. Visitor parking is available on a "
            + "first-come basis at the hourly rate posted at the entrance. Unauthorised or abandoned vehicles may be clamped. ",
            "泊車位只供已登記的車主使用，不得分租他人。車輛須展示有效泊車許可證。停車場內時速限制為每小時十公里。"
            + "訪客泊車位先到先得，收費按入口所示時租計算。未經許可停泊或被棄置的車輛，管理處可加鎖車輪或拖走，"
            + "費用由車主承擔。");

        doc("BL-WATER", "Water supply and tank cleaning / 供水及水箱清洗", "Water",
            "The fresh water tanks are cleaned and disinfected twice a year as required by law. Notice of any temporary "
            + "water suspension is given at least three days in advance through the notification system. Residents are "
            + "advised to store sufficient water before a scheduled suspension. Report any discoloured water to the office. ",
            "食水水箱按法例規定每年清洗及消毒兩次。如需暫停供水，管理處會透過通知系統於最少三天前公布。"
            + "住戶宜於停水前貯存足夠食水。若發現食水變色、有異味或有雜質，請立即通知管理處跟進，"
            + "管理處會安排抽取水辦化驗。");

        doc("BL-MEETING", "Owners' Corporation general meetings (BMO) / 業主立案法團業主大會", "Meetings",
            "Under the Building Management Ordinance (Cap. 344), notice of an Owners' Corporation general meeting must "
            + "be given to all owners at least 14 days before the meeting, stating the date, time, venue and agenda. "
            + "An owner unable to attend may appoint a proxy in writing. Resolutions are passed by a majority of votes "
            + "of the owners present and voting. ",
            "根據《建築物管理條例》（第344章），業主立案法團召開業主大會，須於會議舉行前最少十四天向全體業主"
            + "發出通知，並列明會議日期、時間、地點及議程。未能出席的業主可以書面委任代表代為投票。"
            + "決議以出席並投票的業主的過半數票通過。會議紀錄須供業主查閱。");

        doc("BL-WASTE", "Refuse disposal and recycling / 垃圾棄置及回收", "Waste",
            "Household refuse must be bagged and placed in the refuse room on each floor, not left in the corridor. "
            + "Recyclables (paper, plastic, metal) should be sorted into the recycling bins on the ground floor. "
            + "Large items such as furniture require prior arrangement with the management office for disposal. ",
            "家居垃圾必須妥為袋裝並棄置於各層的垃圾房內，切勿放在走廊。可回收物品（廢紙、塑膠、金屬）"
            + "應分類投放於地下的回收箱。傢俬等大型廢物須事先與管理處預約安排棄置，"
            + "住戶不得自行棄置於公用地方，違者須承擔清理費用。");

        doc("BL-SUBLET", "Subletting and tenancy registration / 分租及租約登記", "Tenancy",
            "Owners who let their flat must register the tenancy and the occupants with the management office and ensure "
            + "tenants observe the estate bylaws. The owner remains responsible for the management fees and for the "
            + "conduct of their tenants. Short-term holiday letting is not permitted. ",
            "業主將單位出租，須向管理處登記租約及所有住客資料，並確保租客遵守屋苑守則。"
            + "業主仍須負責繳付管理費，並就租客的行為負責。屋苑不容許短期渡假式出租（包括網上短租平台），"
            + "違者管理處會終止其門禁卡並轉交法團處理。");

        doc("BL-COMMON", "Use of common areas and podium / 公用地方及平台使用", "Common area",
            "Common areas including the podium garden and sitting-out areas are for the quiet enjoyment of all residents. "
            + "Commercial activities, hawking, and the erection of structures are not allowed. Children must be supervised "
            + "in the playground. The function room may be booked through the management office. ",
            "公用地方包括平台花園及休憩處，供全體住戶安靜享用。場內不得進行商業活動、擺賣或搭建任何構築物。"
            + "兒童使用遊樂場時必須有成人陪同看管。多用途會議室可向管理處預約使用，"
            + "使用後須回復原狀，如有損壞須照價賠償。");

        doc("BL-AC", "Air-conditioner installation and condensate / 冷氣機安裝及冷凝水", "Air-conditioning",
            "Air-conditioners must be installed on the designated platform and fitted with a proper drainage pipe so that "
            + "condensate does not drip onto the unit below or into the common corridor. Dripping air-conditioners are a "
            + "common source of disputes; the resident responsible must rectify the drainage promptly upon notice. ",
            "冷氣機必須安裝於指定的冷氣機位，並須接駁妥當的排水管，確保冷凝水不會滴落下層單位或公用走廊。"
            + "冷氣機滴水是鄰里糾紛的常見成因，亦可能違反《公眾衞生及市政條例》。"
            + "接獲通知的住戶須盡快修妥排水，否則管理處可轉介食環署跟進。");

        // ---- FAQ ------------------------------------------------------------------------------

        doc("FAQ-MOVE", "How to move in or out / booking the lift / 搬入搬出及預約升降機", "FAQ",
            "To move furniture in or out, book the service lift with the management office at least one working day in "
            + "advance. Moving is allowed between 09:00 and 18:00. Lift protection padding will be installed. A refundable "
            + "deposit may be required against damage to common areas during the move. ",
            "搬運傢俬進出單位，須最少於一個工作天前向管理處預約載貨升降機。搬運時間為上午九時至下午六時。"
            + "管理處會為升降機加裝保護墊。搬運期間如損毀公用地方，費用將於按金中扣除，"
            + "故管理處可要求先繳付可退還按金。");

        doc("FAQ-LEAK", "Water seepage between flats — what to do / 單位之間滲水的處理", "FAQ",
            "If water is seeping from the flat above, report it to the management office immediately. The office will "
            + "arrange a joint inspection of both flats to locate the source. Seepage is often caused by a failed waterproof "
            + "membrane or a leaking pipe. The owner of the flat where the defect originates is responsible for the repair. ",
            "如懷疑樓上單位滲水，請立即通知管理處。管理處會安排上下單位進行聯合檢查，以找出滲水源頭。"
            + "滲水成因多為防水層失效或水管滲漏。滲水源頭所在單位的業主須負責維修，"
            + "並須賠償對其他單位造成的損失。住戶應拍照及記錄滲水情況以作日後憑證。");

        doc("FAQ-LOCKOUT", "Locked out or lost keys / 反鎖或遺失鎖匙", "FAQ",
            "The management office does not hold spare keys to private flats for security reasons. If you are locked out, "
            + "you will need to engage a licensed locksmith at your own cost. Please update your contact details with the "
            + "office so we can reach you in an emergency. ",
            "基於保安理由，管理處不會保管私人單位的後備鎖匙。住戶如被反鎖門外，須自行聘請持牌鎖匠開鎖，"
            + "費用自付。管理處可協助核實住戶身分。請住戶保持在管理處登記的聯絡電話為最新，"
            + "以便緊急時聯絡。");

        doc("FAQ-EMERGENCY", "What to do in a fire or typhoon / 火警或颱風應變", "FAQ",
            "In a fire, evacuate immediately via the nearest staircase, never the lift, and assemble at the basketball "
            + "court. During a Typhoon Signal No. 8 or Black Rainstorm Warning, stay indoors, secure windows, and avoid "
            + "the podium and open areas; non-emergency estate services are suspended. Follow instructions sent through "
            + "the Smart Estate notification system. ",
            "發生火警時，請立即由最近的樓梯疏散，切勿使用升降機，並到籃球場集合。"
            + "懸掛八號或以上颱風信號或黑色暴雨警告期間，請留在室內、關好門窗，避免前往平台及空曠地方；"
            + "非緊急的屋苑服務會暫停。請留意透過智能屋苑通知系統發出的指示。");

        // ---- Procedural / liability documents ---------------------------------------------------
        // These carry the escalation paths, responsibility splits and deadlines that harder,
        // situational questions need. Without them a "what are my rights?" question has nothing
        // substantive to retrieve and can only be answered by quoting a topic rule.

        doc("BL-COMPLAINT", "Complaints, escalation and dispute resolution / 投訴、升級處理及糾紛解決", "Complaints",
            "Complaints about a neighbour should first be reported to the management office, which will log the case with "
            + "a reference number and issue a written notice to the flat concerned. If the nuisance continues, the office "
            + "will escalate to a second written warning, then refer the matter to the Owners' Corporation management "
            + "committee. Residents may contact the Police directly on 999 where noise occurs between 23:00 and 07:00, or "
            + "report to the Environmental Protection Department under the Noise Control Ordinance (Cap. 400); the estate "
            + "cannot compel entry to a private flat. Unresolved disputes between owners may be referred to the Lands "
            + "Tribunal. The management office aims to acknowledge a complaint within 2 working days and to give a "
            + "substantive reply within 7. ",
            "投訴鄰居滋擾，應先向管理處備案，管理處會編配個案編號，並向有關單位發出書面通知。"
            + "若滋擾持續，管理處會發出第二次書面警告，再將個案轉交業主立案法團管理委員會處理。"
            + "如噪音發生於晚上十一時至早上七時，住戶可直接致電999報警，亦可根據《噪音管制條例》（第400章）"
            + "向環境保護署投訴。管理處無權強行進入私人單位。業主之間未能和解的糾紛，可交由土地審裁處裁決。"
            + "管理處目標在兩個工作天內確認收到投訴，並在七個工作天內作實質回覆。");

        doc("BL-LIABILITY", "Damage, liability and insurance between flats / 單位之間的損壞、責任及保險", "Liability",
            "The owner of the flat where a defect originates is responsible for repairing it and for making good damage "
            + "caused to another flat. Where the source cannot be agreed, either party may commission an independent "
            + "surveyor; the cost is normally shared until responsibility is established. The Owners' Corporation carries "
            + "third-party and public liability insurance for the common parts only — it does not cover the contents or "
            + "finishes of a private flat, so owners are strongly advised to hold their own home insurance. Claims against "
            + "a neighbour are a civil matter between the two owners; the management office may mediate and provide "
            + "inspection records but is not a party to the claim. Damage caused to common parts by a contractor is "
            + "recoverable from the owner who engaged them, normally out of the renovation deposit. ",
            "缺陷源頭所在單位的業主，須負責維修並賠償對其他單位造成的損壞。若雙方就滲漏源頭未能達成共識，"
            + "任何一方可自行聘請獨立測量師檢驗；在責任未確定前，費用一般由雙方分擔。"
            + "業主立案法團就公用地方投購第三者及公眾責任保險，並不承保私人單位內的財物及裝修，"
            + "故強烈建議業主自行投購家居保險。向鄰居索償屬兩名業主之間的民事事宜，"
            + "管理處可從中調解並提供檢查紀錄，但並非索償的一方。承辦商損毀公用地方所引致的費用，"
            + "管理處會向聘用該承辦商的業主追討，一般於裝修按金中扣除。");

        doc("BL-RENO-DEPOSIT", "Renovation deposit, contractor conduct and forfeiture / 裝修按金、承辦商行為及沒收", "Renovation",
            "A refundable renovation deposit of HK$10,000 is payable before works begin and is held against damage to "
            + "common parts, blocked drains, debris left in corridors, and works carried out outside permitted hours. The "
            + "office will deduct the cost of making good from the deposit and refund the balance within 30 days of the "
            + "works being signed off. Repeated breaches — in particular noisy works on a Sunday or public holiday, which "
            + "are never permitted — may result in the permit being suspended and the full deposit being forfeited. The "
            + "owner remains liable for any cost exceeding the deposit. Contractors must be registered with the office, "
            + "carry public liability insurance, and use the designated service lift only. ",
            "裝修工程開始前，業主須繳付港幣一萬元的可退還按金，以擔保不會損毀公用地方、淤塞排水渠、"
            + "在走廊遺留廢料，或在許可時間以外施工。管理處會從按金中扣除修復費用，"
            + "並於工程驗收後三十天內退還餘額。若屢次違規——尤其在星期日或公眾假期進行噪音工程"
            + "（此乃一律禁止）——管理處可暫停其裝修許可證並沒收全數按金。"
            + "若損毀費用超出按金，業主仍須補足差額。承辦商必須先在管理處登記、"
            + "持有有效公眾責任保險，並只可使用指定的載貨升降機。");

        doc("BL-SUBLET-FEES", "Who pays what when a flat is let / 出租單位的費用責任", "Tenancy",
            "Under the Deed of Mutual Covenant the registered OWNER remains liable to the Owners' Corporation for the "
            + "management fee, regardless of any agreement with a tenant. A tenancy agreement may require the tenant to "
            + "reimburse the owner, but that is a private contract and does not transfer the DMC liability — arrears are "
            + "always pursued against the owner. Government rates and Government rent likewise remain the owner's "
            + "liability unless the tenancy states otherwise. Utilities are normally registered in the tenant's name. Only "
            + "registered owners may vote at an Owners' Corporation general meeting; a tenant may attend only as an "
            + "observer or with a written proxy from the owner. ",
            "根據公契，繳付管理費的責任由登記業主承擔，即使租約訂明由租客支付亦然。"
            + "租約可要求租客向業主償付管理費，但那只是業主與租客之間的私人合約，並不轉移公契下的責任；"
            + "追討欠繳管理費，法團一律向業主追討。差餉及地租同樣由業主負責，除非租約另有訂明。"
            + "水、電、煤氣等公用事業費用一般以租客名義登記。只有登記業主方可在業主大會投票，"
            + "租客只可以旁聽者身分出席，或憑業主的書面委任代表出席。");

        doc("BL-OWNER-RIGHTS", "Owners' rights under the DMC and BMO / 業主在公契及建築物管理條例下的權利", "Meetings",
            "Under the Building Management Ordinance (Cap. 344) an owner may inspect the Owners' Corporation's accounts, "
            + "budgets, minutes and the register of owners, and may request copies at a reasonable copying charge. Owners "
            + "holding not less than 5% of the shares may requisition a general meeting. Decisions on expenditure above "
            + "the limits in the DMC require a resolution of a general meeting. An owner who considers a resolution or a "
            + "management decision improper may apply to the Lands Tribunal. The management company acts as agent of the "
            + "Owners' Corporation and must produce audited accounts annually. Notice of a general meeting must be given "
            + "at least 14 days in advance, and 45 days for a meeting to appoint or remove a management committee member. ",
            "根據《建築物管理條例》（第344章），業主有權查閱業主立案法團的帳目、財政預算、會議紀錄及業主名冊，"
            + "並可按合理影印費索取副本。持有不少於百分之五份數的業主，可要求召開業主大會。"
            + "超出公契所訂限額的開支，須經業主大會決議通過。業主如認為某項決議或管理決定不當，"
            + "可向土地審裁處提出申請。管理公司以法團代理人的身分行事，並須每年提交經審計的帳目。"
            + "業主大會須於最少十四天前發出通知；如涉及委任或罷免管理委員會委員，則須四十五天前發出。");

        // ---- Security, safety and statutory obligations ----------------------------------------

        doc("BL-SECURITY", "Estate security, visitors and access cards / 屋苑保安、訪客及門禁卡", "Security",
            "Each flat is issued two access cards; additional or replacement cards cost HK$150 each and must be applied "
            + "for by the registered owner or tenant. Visitors must register at the guard counter and be verified with the "
            + "flat before entry. Contractors and delivery staff are issued a temporary pass against an identity document. "
            + "Lost cards must be reported immediately so they can be deactivated. Tailgating through the lobby gate defeats "
            + "the access control and is a breach of the house rules. ",
            "每個單位獲發兩張門禁卡；額外或補領的門禁卡每張收費港幣一百五十元，須由登記業主或租客申請。"
            + "訪客必須於保安櫃檯登記，並經有關單位確認身分後方可進入。承辦商及送貨人員須憑身分證明文件"
            + "換領臨時通行證。門禁卡如有遺失，須立即通知管理處註銷，以防被人盜用。"
            + "尾隨他人進入大堂閘門會令門禁系統形同虛設，屬違反屋苑守則。");

        doc("BL-CCTV", "CCTV coverage and personal data / 閉路電視及個人資料", "Privacy",
            "CCTV cameras cover the lobbies, lifts, car park and perimeter for security purposes only. Recordings are kept "
            + "for 31 days and then overwritten automatically. Under the Personal Data (Privacy) Ordinance (Cap. 486) a "
            + "resident may request access to footage in which they appear by submitting a data access request to the "
            + "management office; a fee may be charged and other individuals may be masked. Footage is released to the "
            + "Police on lawful request. Cameras are not installed inside private flats or in changing areas. ",
            "閉路電視覆蓋大堂、升降機、停車場及屋苑外圍，僅作保安用途。錄影紀錄保存三十一天，"
            + "其後自動覆寫。根據《個人資料（私隱）條例》（第486章），住戶可向管理處提交查閱資料要求，"
            + "索取載有其本人影像的片段；管理處可收取合理費用，並會遮蓋片段中其他人士的容貌。"
            + "如警方依法要求，管理處會提供有關片段。私人單位內及更衣室範圍一概不設攝影機。");

        doc("BL-LIFT", "Lift service, maintenance and entrapment / 升降機服務、保養及被困處理", "Lifts",
            "Lifts are serviced monthly by a registered lift contractor and undergo a statutory annual examination; notice "
            + "of any lift being taken out of service is posted at the lobby. If you are trapped in a lift, stay calm, do "
            + "not attempt to force the doors, press the alarm or intercom button and wait for the rescue team — never try "
            + "to climb out. Do not overload the lift or use the passenger lift to carry renovation debris or furniture; "
            + "the service lift must be booked for that. ",
            "升降機由註冊升降機承辦商每月保養，並須接受法定的周年檢驗；如某部升降機需暫停服務，"
            + "管理處會在大堂張貼通知。若被困升降機內，請保持鎮定，切勿強行扳開機門，"
            + "應按下警鐘或對講機按鈕求助，並在機廂內等候救援人員，切勿自行爬出機廂，以免發生意外。"
            + "請勿超載，亦不可使用客用升降機搬運裝修廢料或傢俬，該等搬運須預約載貨升降機。");

        doc("BL-FSI", "Fire service installations and annual inspection / 消防裝置及周年檢查", "Fire safety",
            "Fire service installations — sprinklers, hose reels, fire alarms, emergency lighting and extinguishers — are "
            + "inspected annually by a registered fire service installation contractor, who issues an FS 251 certificate to "
            + "the Fire Services Department. Residents must not obstruct, cover or tamper with any installation, and must "
            + "allow access to their flat for inspection where a sprinkler head or detector is inside the unit. Fire doors "
            + "on escape routes must be kept closed and never wedged open. ",
            "消防裝置包括自動花灑系統、消防喉轆、火警警報系統、緊急照明及滅火筒，均由註冊消防裝置承辦商"
            + "每年檢驗，並向消防處提交消防裝置證明書（FS 251表格）。住戶不得阻塞、遮蓋或擅自改動任何消防裝置。"
            + "若單位內裝有花灑頭或偵煙器，住戶須在檢查時提供進入單位的方便。"
            + "走火通道上的防煙門必須保持關閉，嚴禁以雜物撐開。");

        doc("BL-ALARM", "False fire alarms and charges / 誤鳴火警警報及收費", "Fire safety",
            "The fire alarm is monitored 24 hours. Alarms caused by cooking smoke, steam, or renovation dust are the most "
            + "common false activations. Where a false alarm is traced to a resident's negligence — for example a detector "
            + "covered during renovation, or smoking under a detector — the cost of resetting the system and any Fire "
            + "Services Department attendance may be recharged to that flat. Deliberately activating a break-glass unit "
            + "without cause is an offence. On hearing the alarm, evacuate by the staircase and do not use the lift. ",
            "火警警報系統設有二十四小時監察。最常見的誤鳴成因是煮食油煙、蒸氣及裝修塵埃。"
            + "若誤鳴警報是由住戶疏忽引致——例如裝修期間遮蓋偵煙器，或在偵煙器下吸煙——"
            + "重新啟動系統的費用及消防處出動的相關費用，管理處可向該單位追討。"
            + "在沒有火警的情況下故意打破手動報警器屬刑事罪行。聽到警報時，請由樓梯疏散，切勿使用升降機。");

        doc("BL-WINDOW", "Window and external wall inspection (MBIS / MWIS) / 強制驗樓及強制驗窗計劃", "Building safety",
            "Under the Mandatory Building Inspection Scheme and Mandatory Window Inspection Scheme, buildings aged 10 years "
            + "or above may be served a statutory notice by the Buildings Department requiring inspection and any necessary "
            + "repair of the common parts and of windows in individual flats. When the estate is served a notice, the "
            + "Owners' Corporation appoints a Registered Inspector for the common parts, while each owner remains "
            + "responsible for the windows of their own flat. Failure to comply is an offence carrying a fine. ",
            "根據強制驗樓計劃及強制驗窗計劃，樓齡達十年或以上的樓宇，屋宇署可送達法定通知，"
            + "要求檢驗並維修公用地方及各單位的窗戶。屋苑接獲通知後，業主立案法團會委任註冊檢驗人員"
            + "負責公用部分，而各單位窗戶的檢驗及維修，則由該單位的業主自行負責及承擔費用。"
            + "不遵從法定通知屬刑事罪行，可被罰款，屋宇署亦可代為進行工程並向業主收回費用。");

        doc("BL-BALCONY", "Balconies, drying racks and falling objects / 露台、晾衣架及高空擲物", "Building safety",
            "Objects must never be placed on window ledges, balcony railings or air-conditioner platforms where they could "
            + "fall. Drying poles and racks must be securely fixed and are not permitted to overhang a public area. "
            + "Throwing anything from a height is a serious criminal offence and is also grounds for the management office "
            + "to refer the matter to the Police; CCTV and, where necessary, temporary monitoring cameras may be deployed "
            + "in response to repeated incidents. ",
            "住戶切勿在窗台、露台欄杆或冷氣機平台上擺放任何可能墮下的物件。晾衣杆及晾衣架必須穩固安裝，"
            + "並不得伸出至公眾地方上空。高空擲物屬嚴重刑事罪行，可判處監禁，管理處會將個案轉交警方處理。"
            + "如同一位置屢次發生高空墮物，管理處可加裝閉路電視或臨時監察攝影機以追查責任人。");

        doc("BL-UBW", "Unauthorised building works / 僭建物", "Building safety",
            "Structures erected without approval from the Buildings Department — enclosed balconies, metal cages, canopies, "
            + "rooftop or podium structures, and removal of structural walls — are unauthorised building works and may be "
            + "the subject of a removal order. The management office must report suspected works affecting the common parts "
            + "or structure. An owner who buys a flat containing existing unauthorised works inherits the liability for "
            + "them. Alterations to drains, windows or external walls always require prior approval. ",
            "未經屋宇署批准而搭建的構築物——例如封閉露台、鐵籠、簷篷、天台或平台構築物，以及拆除主力牆——"
            + "均屬僭建物，屋宇署可發出清拆令。若懷疑工程影響公用地方或樓宇結構，管理處必須向屋宇署舉報。"
            + "業主購入的單位如已存在僭建物，須一併承擔清拆責任，故買樓前應查冊及實地檢視。"
            + "改動排水渠、窗戶或外牆，一律須事先取得批准。");

        doc("BL-SLOPE", "Slopes and retaining walls / 斜坡及擋土牆", "Building safety",
            "Slopes and retaining walls within the estate boundary are maintained by the Owners' Corporation and inspected "
            + "annually before the wet season under the Geotechnical Engineering Office's maintenance guidelines. Residents "
            + "must not plant, excavate, dump material or discharge water onto a slope, as this can undermine stability. "
            + "Signs of distress — cracks, bulging, seepage, or blocked slope drainage channels — should be reported to the "
            + "management office immediately, particularly after heavy rain. ",
            "屋苑範圍內的斜坡及擋土牆由業主立案法團負責維修，並按土力工程處的維修指引，"
            + "於每年雨季前進行檢查。住戶不得在斜坡上種植、挖掘、傾倒雜物或引水排放，以免影響斜坡穩定。"
            + "若發現斜坡出現裂縫、隆起、滲水，或排水渠淤塞等危險徵象，尤其在暴雨之後，"
            + "應立即通知管理處派員檢查。");

        // ---- Facilities and daily living --------------------------------------------------------

        doc("BL-CLUB", "Clubhouse, swimming pool and fitness room / 會所、泳池及健身室", "Facilities",
            "Clubhouse facilities are for residents and their accompanied guests; each flat may bring up to two guests, and "
            + "a guest fee applies. The swimming pool operates from April to October, is supervised by a lifeguard during "
            + "posted hours, and is closed when no lifeguard is on duty or when Typhoon Signal No. 3 or above is hoisted. "
            + "Children under 12 must be accompanied by an adult. The fitness room is restricted to residents aged 16 and "
            + "above. Bookings are made through the management office and may be cancelled up to 24 hours in advance. ",
            "會所設施供住戶及其陪同的訪客使用；每個單位最多可攜同兩名訪客，並須繳付訪客費用。"
            + "泳池開放期為四月至十月，於公布的開放時間內設救生員當值；如未有救生員當值，"
            + "或懸掛三號或以上颱風信號，泳池即會關閉。十二歲以下兒童必須由成人陪同。"
            + "健身室只限十六歲或以上住戶使用。各項設施須向管理處預約，並可於使用前二十四小時取消。");

        doc("BL-BBQ", "Barbecue, open flame and gas safety / 燒烤、明火及氣體安全", "Facilities",
            "Barbecues are permitted only at the designated podium pits, must be booked in advance, and must be "
            + "extinguished by 22:00. Open flames, charcoal and portable gas stoves are prohibited on balconies, in "
            + "corridors and in the clubhouse. Gas cylinders may not be stored in a flat, in the car park or in any common "
            + "area. Towngas installations must be maintained by a registered gas contractor; report any smell of gas to "
            + "the management office and the Towngas emergency hotline immediately, and do not operate electrical switches. ",
            "燒烤只可於平台指定的燒烤爐進行，須事先預約，並須於晚上十時前完全熄滅爐火。"
            + "露台、走廊及會所範圍內一律禁止使用明火、木炭及輕便石油氣爐。"
            + "石油氣樽不得存放於單位內、停車場或任何公用地方。煤氣裝置須由註冊氣體承辦商保養；"
            + "如嗅到懷疑煤氣洩漏氣味，請立即通知管理處及致電煤氣公司緊急熱線，"
            + "切勿開關任何電掣，並打開窗戶保持通風。");

        doc("BL-SMOKING", "Smoking in the estate / 屋苑內吸煙規定", "Common area",
            "Under the Smoking (Public Health) Ordinance (Cap. 371) smoking is prohibited in all indoor common areas "
            + "including lobbies, lifts, corridors, staircases, the clubhouse and the car park. Fixed penalty notices may "
            + "be issued by Tobacco and Alcohol Control Office inspectors. Smoking on a private balcony is not unlawful but "
            + "is a frequent source of complaints where smoke drifts into a neighbour's flat; residents are asked to be "
            + "considerate, and persistent nuisance may be handled under the estate's complaint procedure. ",
            "根據《吸煙（公眾衞生）條例》（第371章），所有室內公用地方均禁止吸煙，"
            + "包括大堂、升降機、走廊、樓梯、會所及停車場。控煙酒辦公室的控煙督察可發出定額罰款通知書。"
            + "在私人露台吸煙並不違法，但若煙味飄入鄰居單位，往往引起投訴；"
            + "請住戶互相體諒，如滋擾持續，可按屋苑的投訴程序處理。");

        doc("BL-DELIVERY", "Deliveries, couriers and the parcel room / 送遞、速遞及包裹存放", "FAQ",
            "Couriers may not proceed to a flat without the resident's confirmation. Parcels can be left at the management "
            + "counter and are held for 7 days; the office is not a bailee and does not accept liability for the contents "
            + "of unattended parcels, perishables, or items of high value. Bulky deliveries and furniture must use the "
            + "service lift, booked one working day in advance. Food delivery riders are directed to the ground floor "
            + "lobby only. ",
            "速遞員未經住戶確認，不得自行前往單位。包裹可暫存管理處櫃檯，保管期為七天；"
            + "管理處並非保管人，對無人看管的包裹內容、易腐壞物品或貴重物品概不負責。"
            + "大型貨品及傢俬須使用載貨升降機，並於一個工作天前預約。"
            + "外賣速遞員只可到達地下大堂，由住戶自行下樓領取。");

        doc("BL-EV", "Electric vehicle charging in the car park / 停車場電動車充電", "Car park",
            "Charging is permitted only at installed EV charging points; running a cable from a private flat or a common "
            + "socket to a vehicle is prohibited and is an electrical safety hazard. Owners wishing to install a charger at "
            + "their own space must apply to the management office with the contractor's proposal and a registered "
            + "electrical worker's certification, and the loading must be verified against the building's supply capacity. "
            + "Charging bays are for active charging only; a vehicle left connected after charging completes may be moved. ",
            "電動車只可在已安裝的充電設施充電；嚴禁從私人單位或公用電插座拉電線為車輛充電，"
            + "此舉構成嚴重電力安全風險。業主如欲在自用車位安裝充電器，須向管理處提交承辦商方案"
            + "及註冊電業工程人員的證明書提出申請，並須核實大廈供電容量是否足以承載。"
            + "充電車位只供正在充電的車輛使用；充電完成後仍佔用車位的車輛，管理處可安排移走。");

        doc("BL-PARCEL-BIKE", "Bicycles, prams and belongings in common areas / 單車、嬰兒車及公用地方雜物", "Common area",
            "Bicycles must be kept in the bicycle parking area on the ground floor and may not be stored in corridors, "
            + "staircases, lift lobbies or against fire doors, all of which are means of escape. Prams and mobility aids "
            + "may be left immediately outside a flat entrance only where they do not reduce the escape route width. Items "
            + "left in a common area will be tagged with a removal notice and may be disposed of after 14 days at the "
            + "owner's cost. Storage of flammable material in any common area is prohibited outright. ",
            "單車必須停放於地下的單車停放處，不得存放於走廊、樓梯、升降機大堂或防煙門旁，"
            + "因該等位置均屬走火通道。嬰兒車及助行器只可在不影響走火通道闊度的情況下，"
            + "暫放於單位門外。放置於公用地方的雜物，管理處會貼上清理通知，"
            + "十四天後可予以清走，費用由物主承擔。任何公用地方一律嚴禁存放易燃物品。");

        doc("BL-LOST", "Lost property / 失物認領", "FAQ",
            "Items found in the common areas are logged and held at the management office for 3 months, after which "
            + "unclaimed property is donated or disposed of. Identity documents, bank cards and keys are passed to the "
            + "Police. To claim an item you must describe it before it is shown to you and sign the property register. "
            + "The estate does not accept liability for property lost within the estate. ",
            "在公用地方拾獲的物品會記錄在案並存放於管理處三個月，逾期無人認領者會捐贈或棄置。"
            + "身分證明文件、銀行卡及鎖匙會交予警方處理。認領失物時，住戶須先描述物品特徵，"
            + "核對無誤後方獲發還，並須在失物登記冊上簽署。屋苑對在範圍內遺失的財物概不負責。");

        // ---- Hygiene, environment and services --------------------------------------------------

        doc("BL-DRAIN", "Drainage, blockages and illegal discharge / 排水渠、淤塞及非法排放", "Drainage",
            "Only water, human waste and toilet paper may be discharged into the drainage system. Cooking oil, cement "
            + "slurry, renovation waste, wet wipes and cat litter cause blockages and backflow into lower flats. Where a "
            + "blockage is traced to a particular flat, the cost of clearing it and of any resulting damage is recharged to "
            + "that flat. Owners are responsible for the drain pipes serving their own flat up to the common stack. "
            + "Discharging waste water into a rainwater drain is an offence under the Water Pollution Control Ordinance. ",
            "排水系統只可排放污水、排泄物及廁紙。食油、英泥漿、裝修廢料、濕紙巾及貓砂均會造成渠道淤塞，"
            + "並令污水倒灌至低層單位。若淤塞源頭確定為某一單位，通渠費用及因此造成的損壞賠償，"
            + "會向該單位追討。業主須負責由其單位接駁至公用渠管之間的所有排水管。"
            + "將污水排入雨水渠，屬違反《水污染管制條例》的罪行。");

        doc("BL-SEEPAGE-JO", "Seepage that cannot be resolved — Joint Office referral / 滲水個案轉介聯合辦事處", "FAQ",
            "Where the source of seepage cannot be established or the flat concerned refuses access, the affected owner may "
            + "refer the case to the Joint Office of the Food and Environmental Hygiene Department and the Buildings "
            + "Department, which can investigate and, where seepage constitutes a nuisance, serve a nuisance notice under "
            + "the Public Health and Municipal Services Ordinance (Cap. 132). The management office will provide inspection "
            + "records to support the referral but has no power to compel entry to a private flat. ",
            "若無法確定滲水源頭，或有關單位拒絕讓人入內檢查，受影響的業主可將個案轉介"
            + "食物環境衞生署與屋宇署合設的滲水投訴調查聯合辦事處。聯合辦事處有權進行調查，"
            + "如認定滲水構成妨擾，可根據《公眾衞生及市政條例》（第132章）發出妨擾事故通知。"
            + "管理處會提供已進行的檢查紀錄以支持轉介，但管理處本身無權強行進入私人單位。");

        doc("BL-PEST", "Pest and mosquito control / 蟲鼠及蚊患控制", "Hygiene",
            "Common areas are treated for pests monthly and inspected weekly for stagnant water during the wet season. "
            + "Residents must not leave water in saucers, buckets or planters on balconies, which breed Aedes mosquitoes. "
            + "Refuse must be bagged and sealed before disposal. Where an infestation is traced to conditions inside a "
            + "flat, the office will give notice to the occupant to rectify; persistent cases may be reported to the Food "
            + "and Environmental Hygiene Department. Treatment inside a private flat is arranged at the occupant's cost. ",
            "公用地方每月進行滅蟲滅鼠，並於雨季期間每週巡查有否積水。住戶不得在露台的花盆碟、"
            + "水桶或盆栽內積水，以免滋生白紋伊蚊。垃圾必須妥為袋裝密封後才棄置。"
            + "若蟲患源頭源於單位內的環境衞生問題，管理處會發出通知要求住客改善；"
            + "情況持續者可轉介食物環境衞生署跟進。私人單位內的滅蟲工作由住客自行安排及付費。");

        doc("BL-GREEN", "Recycling programme and energy saving / 回收計劃及節能措施", "Waste",
            "Separate bins for paper, plastic, metal and glass are provided on the ground floor, and a rechargeable battery "
            + "and fluorescent lamp collection point is located next to the management office. Waste electrical and "
            + "electronic equipment is collected on the first Saturday of each month by prior arrangement. Common area "
            + "lighting is on motion sensors and timers; residents are asked not to override them. The estate reports "
            + "annual recycling tonnage to owners at the general meeting. ",
            "地下設有廢紙、塑膠、金屬及玻璃樽分類回收箱，管理處旁另設充電池及慳電膽回收點。"
            + "廢電器及電子產品於每月第一個星期六收集，住戶須事先預約。"
            + "公用地方照明已裝設動態感應器及定時器，請住戶勿自行更改設定。"
            + "屋苑每年於業主大會向業主報告回收量及節能成效。");

        // ---- Money, governance and management ---------------------------------------------------

        doc("BL-SINKING", "Special fund, maintenance levy and annual budget / 特別基金、維修徵費及年度預算", "Fees",
            "In addition to the monthly management fee, owners contribute to a special fund (sinking fund) that pays for "
            + "major periodic works such as external wall repair, lift modernisation and water tank replacement. The annual "
            + "budget and the audited accounts are circulated to owners before the general meeting and must be available "
            + "for inspection. A special levy for works beyond the fund's balance requires a resolution of a general "
            + "meeting. Contributions are apportioned by the shares allocated to each flat under the Deed of Mutual "
            + "Covenant, not by floor area occupied. ",
            "除每月管理費外，業主亦須供款至特別基金（俗稱維修基金），用以支付大型定期工程，"
            + "例如外牆維修、升降機更新及水箱更換。年度財政預算及經審計的帳目，"
            + "須於業主大會前送交各業主，並供業主查閱。若工程開支超出基金結餘而需徵收特別徵費，"
            + "必須經業主大會決議通過。各單位的供款按公契所分配的份數攤分，而非按實用面積計算。");

        doc("BL-PROCURE", "Procurement, tendering and large works / 採購、招標及大型工程", "Meetings",
            "Under Schedule 7 of the Building Management Ordinance, procurement of goods or services exceeding the "
            + "prescribed financial limits must go to open tender, and the Owners' Corporation must invite tenders publicly "
            + "and report the results to owners. Contracts above the higher threshold require approval by resolution at a "
            + "general meeting. Owners may inspect the tender documents and the management committee's assessment. The "
            + "management company may not accept any commission or rebate from a contractor. ",
            "根據《建築物管理條例》附表7，採購貨品或服務若超逾訂明的款額，必須公開招標，"
            + "業主立案法團須公開邀請投標，並將結果通知業主。若合約金額超逾較高的門檻，"
            + "更須經業主大會決議通過。業主有權查閱標書及管理委員會的評標報告。"
            + "管理公司不得向承辦商收取任何佣金或回佣，違者可構成失當行為。");

        doc("BL-STAFF", "Estate staff, patrols and service standards / 屋苑職員、巡邏及服務標準", "Management",
            "The management office counter is staffed from 08:00 to 22:00 daily, with a security officer on duty 24 hours "
            + "and foot patrols of the podium, car park and refuse rooms every two hours overnight. Staff are not permitted "
            + "to enter a private flat unaccompanied, to hold flat keys, to accept gratuities, or to carry out private work "
            + "for residents during duty hours. Complaints about a member of staff should be made in writing to the "
            + "management office and are handled by the manager, with serious matters referred to the management committee. ",
            "管理處櫃檯每日上午八時至晚上十時有職員當值，保安員二十四小時當值，"
            + "夜間每兩小時巡邏平台、停車場及垃圾房一次。職員不得在無人陪同下進入私人單位、"
            + "不得代住戶保管單位鎖匙、不得收受打賞，亦不得在當值時間內為住戶進行私人工作。"
            + "如對職員有投訴，應以書面向管理處提出，由屋苑經理處理；嚴重個案會轉交管理委員會跟進。");

        doc("BL-HELPER", "Domestic helpers and occupancy registration / 家庭傭工及住客登記", "Tenancy",
            "Residents must register the names of all occupants, including foreign domestic helpers, with the management "
            + "office so that emergency notifications reach the right people. Helpers are issued an access card linked to "
            + "the flat, which must be returned when employment ends. Helpers are entitled to use the common areas in the "
            + "course of their duties. Sleeping in a common area, on a rooftop, or in a car park space is not permitted. "
            + "Employers are asked to brief helpers on the refuse disposal, noise and fire evacuation rules. ",
            "住戶須向管理處登記所有住客姓名，包括外籍家庭傭工，以確保緊急通知能送達正確人士。"
            + "傭工會獲發與該單位掛鈎的門禁卡，僱傭關係結束時須交回管理處。"
            + "傭工在執行職務期間有權使用公用設施。任何人不得在公用地方、天台或停車場車位內留宿。"
            + "請僱主向傭工講解垃圾棄置、噪音管制及火警疏散的規定。");

        doc("BL-ACCESS", "Barrier-free access and priority facilities / 無障礙設施及優先使用", "Common area",
            "Ramps, tactile guide paths, the accessible toilet and the priority lift are provided for residents with "
            + "disabilities and must be kept unobstructed at all times. Wheelchair and mobility-aid users have priority in "
            + "the lifts. Assistance dogs are permitted throughout the estate regardless of the general pet rules. "
            + "Residents who would need help evacuating in an emergency are encouraged to register with the management "
            + "office so that the fire evacuation plan accounts for them. ",
            "屋苑設有斜道、引導徑、無障礙洗手間及優先使用的升降機，供有需要的住戶使用，"
            + "必須時刻保持暢通，不得擺放雜物。輪椅及使用助行器的住戶在升降機享有優先權。"
            + "導盲犬及其他輔助犬可在屋苑各處通行，不受一般寵物守則限制。"
            + "如住戶在緊急疏散時需要協助，歡迎向管理處登記，以便在火警疏散計劃中作出安排。");
    }

    /**
     * Seeds one bylaw as a single bilingual document. The English and 繁體中文 renderings of the same
     * rule live together so a citation points at one document whichever language the resident used;
     * RagService chunks them apart and tags each chunk with its own language.
     */
    private void doc(String code, String title, String category, String english, String chinese) {
        BylawDoc d = new BylawDoc();
        d.setCode(code);
        d.setTitle(title);
        d.setLanguage(Language.EN);   // record-level lead language; per-chunk language is detected
        d.setCategory(category);
        d.setContent(english + chinese);
        repo.save(d);
    }
}
