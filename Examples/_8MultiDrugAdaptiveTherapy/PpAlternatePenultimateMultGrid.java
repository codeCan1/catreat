package Examples._8MultiDrugAdaptiveTherapy;

//This script simulates adaptive therapy with replacement and tumor measurement error
// Dividing cells (DOUBLY_SENSITIVE or DOUBLY_RESISTANT) can replace neighboring cells with 0 being no replacement and 1 being replacement at all times
//Resistance is binary phenotype (0 or 1)
//19 June 2019

import Framework.GridsAndAgents.AgentGrid2D;
import Framework.GridsAndAgents.AgentSQ2Dunstackable;
import Framework.GridsAndAgents.PDEGrid2D;
import Framework.Gui.GridWindow;
import Framework.Gui.UIGrid;
import Framework.Gui.UILabel;
import Framework.Gui.UIWindow;
import Framework.Rand;
import Framework.Tools.FileIO;
import Framework.Util;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import static Framework.Util.*;

public class PpAlternatePenultimateMultGrid extends AgentGrid2D<PpAlternatePenultimateMultCell> {
    //model constants
    //cell colors are red for doubly DOUBLY_RESISTANT, green for doubly DOUBLY_SENSITIVE, orange for RESISTANT to Drug A but not B, and brown for RESISTANT to Drug B but not A
    public final static int DOUBLY_RESISTANT = CategorialColor(1), RESISTANT_TO_DRUG_A = CategorialColor(5), RESISTANT_TO_DRUG_B = CategorialColor(8), DOUBLY_SENSITIVE = CategorialColor(2);
    public double DIV_PROB_SEN, DIV_PROB_RES, DIV_PROB_RES_DRUG_A, DIV_PROB_RES_DRUG_B, DEATH_PROB, PREV_DRUG_A, PREV_DRUG_B, CURRENT_DRUG_A, CURRENT_DRUG_B;
    public int PREV_POP_DRUG_A, PREV_POP_DRUG_B, CURRENT_POP,CURRENT_POP_DRUG_A,CURRENT_POP_DRUG_B, INITIAL_POP, FINAL_POP;
    public int SEN_POP, RES_POP, RES_DRUG_A_POP, RES_DRUG_B_POP;
    public int DRUG_CYCLE_ITERATOR = 0, DRUG_CYCLE_ITERATOR_AT = 0;
    public int AT_START_TICK;
    public int VARIABLE_BETA;
    public double DRUG_ON_TIME, DRUG_CYCLE_TIME, DRUG_DIFF_RATE, DRUG_UPTAKE, DRUG_A_DEATH, DRUG_B_DEATH;

    //    public double DRUG_ON_TIME = 40, DRUG_OFF_TIME = 160, DRUG_CYCLE_TIME = 200, DRUG_DIFF_RATE = 2.0, DRUG_UPTAKE = 0.91, DRUG_BOUNDARY_VAL = 1.0, DRUG_METABOLISM_TIME = 40.0, DRUG_A_DEATH=0.2, DRUG_B_DEATH=0.2;
    public double AT_ALPHA_DRUG_A, AT_ALPHA_DRUG_B,AT_GAMMA_DRUG_A,AT_GAMMA_DRUG_B, AT_BETA, MAX_TOLERATED_DOSE_A, MAX_TOLERATED_DOSE_B, MIN_DRUG_DOSE_A, MIN_DRUG_DOSE_B;
    public double REPLACEMENT_THRESHOLD;
    public double MEASUREMENT_NOISE_SD;
    public double Mut_DoublySen_to_ResA = 1e-4;
    public double Mut_DoublySen_to_ResB = 1e-4;
    public double Mut_ResA_to_DoublyRes = 1e-4;
    public double Mut_ResB_to_DoublyRes = 1e-4;
    public double Mut_DoublySen_to_DoublyRes = 1e-4;

    public double Mut_DoublyRes_to_ResA = 0;
    public double Mut_DoublyRes_to_ResB = 0;
    public double Mut_DoublyRes_to_DoublySen = 0;
    public double Mut_ResA_to_DoublySen = 0;
    public double Mut_ResB_to_DoublySen = 0;
    //internal model objects
    public PDEGrid2D drugA, drugB;
    public Rand rn;
    public int[] divHood = MooreHood(false);
    public boolean HAS_STANDARD_THERAPY_STARTED = false;
    public boolean HAS_AT_STARTED;
    public int CHECK_TUMORSIZE_INTERVAL_AT;
    public int TUMOR_SIZE_TRIGGERING_AT;
    public boolean IS_STANDARD_THERAPY_ON = false;
    public boolean IS_AT_ON;
    public boolean IS_TREATMENT_VACATION_ON;
    public boolean IS_LAST_DRUG_ADJUSTED_A, IS_LAST_DRUG_ADJUSTED_B,IS_IT_FIRST_TIME_DRUG_A,IS_IT_FIRST_TIME_DRUG_B;
    public boolean IS_DRUG_FROM_PERIPHERY = false;//default is drug is added internally at all grids and to make drug from periphery set the boolean to true


    UILabel tickLabel;
    UILabel popLabel;
    UILabel drugLabel;


    public PpAlternatePenultimateMultGrid(int xDim, int yDim, Rand rn, double birthSensitiveProb, double birthResistantProb, double deathProb, double AT_Alpha_Drug_A, double AT_Alpha_Drug_B, double AT_Gamma_Drug_A,double AT_Gamma_Drug_B, double AT_Beta, double Max_Tolerated_dose_A, double Max_Tolerated_dose_B, double Min_Drug_Dose_A, double Min_Drug_Dose_B, double DRUG_ON_TIME, double DRUG_CYCLE_TIME, double DRUG_DIFF_RATE, double DRUG_UPTAKE, double DRUG_A_DEATH, double DRUG_B_DEATH, int check_tumorsize_interval_AT, double tumor_size_percent_triggering_AT, double replacement_threshold, double measurement_noise_sd,UILabel tickLabel,UILabel popLabel,UILabel drugLabel) {
        super(xDim, yDim, PpAlternatePenultimateMultCell.class);
        this.rn = rn;
        this.DIV_PROB_SEN = birthSensitiveProb;
        this.DIV_PROB_RES = birthResistantProb;
        this.DIV_PROB_RES_DRUG_A = (birthSensitiveProb + birthResistantProb) * 0.5;
        this.DIV_PROB_RES_DRUG_B = (birthSensitiveProb + birthResistantProb) * 0.5;
        this.DEATH_PROB = deathProb;
        this.AT_ALPHA_DRUG_A = AT_Alpha_Drug_A;
        this.AT_ALPHA_DRUG_B = AT_Alpha_Drug_B;
        this.AT_GAMMA_DRUG_A = AT_Gamma_Drug_A;
        this.AT_GAMMA_DRUG_B = AT_Gamma_Drug_B;
        this.AT_BETA = AT_Beta;
        this.MAX_TOLERATED_DOSE_A = Max_Tolerated_dose_A;
        this.MAX_TOLERATED_DOSE_B = Max_Tolerated_dose_B;
        this.MIN_DRUG_DOSE_A = Min_Drug_Dose_A;
        this.MIN_DRUG_DOSE_B = Min_Drug_Dose_B;
        this.DRUG_ON_TIME = DRUG_ON_TIME;
        this.DRUG_CYCLE_TIME = DRUG_CYCLE_TIME;
        this.DRUG_DIFF_RATE = DRUG_DIFF_RATE;
        this.DRUG_UPTAKE = DRUG_UPTAKE;
        this.DRUG_A_DEATH = DRUG_A_DEATH;
        this.DRUG_B_DEATH = DRUG_B_DEATH;

        this.HAS_AT_STARTED = false;
        this.IS_AT_ON = false;
        this.IS_TREATMENT_VACATION_ON = false;
        this.CHECK_TUMORSIZE_INTERVAL_AT = check_tumorsize_interval_AT;
        this.TUMOR_SIZE_TRIGGERING_AT = (int) (tumor_size_percent_triggering_AT * length);
        this.REPLACEMENT_THRESHOLD = replacement_threshold;
        this.MEASUREMENT_NOISE_SD = measurement_noise_sd;
        this.tickLabel = tickLabel;
        this.popLabel = popLabel;
        this.drugLabel = drugLabel;
        drugA = new PDEGrid2D(xDim, yDim);
        drugB = new PDEGrid2D(xDim, yDim);

    }

    public static void main(String[] args) {
        int x = 100, y = 100, visScale = 3, tumorRad = 10, msPause = 0;
        double resistantProp = 0.5;
        UIGrid vis = new UIGrid(x * 4, y, visScale, true);
        PpAlternatePenultimateMultGrid[] models = new PpAlternatePenultimateMultGrid[4];
        String strategy = "DosAdjMult2";
        String sub_strategy = "PpAlt";
        String file_counter = "1" ;
//        BufferedReader userInput=new BufferedReader(new InputStreamReader(System.in));
        FileIO popsOut=new FileIO(strategy+"-"+sub_strategy+"-"+file_counter,"w");
        System.out.println("Please enter in order: 1.Birth Prob DOUBLY_SENSITIVE 2.Birth Prob DOUBLY_RESISTANT 3. Death Prob 4.Alpha Param for AT 5.Beta Param for AT 6.Maximum Tolerated Dose 7. TimeSteps after which tumor size(cell number) is checked for AT 8. AT starts after what fraction of the grid is occupied pressing enter after each");
        double Div_Prob_Sen = 0.08;
        double Div_Prob_Res = 0.02;
        double deathProb = 0.01;
        double AT_alpha_drug_A = 0.5;
        double AT_alpha_drug_B = 0.5;
        double AT_gamma_drug_A=2;
        double AT_gamma_drug_B=2;
        double AT_beta = 0.1;
        double Max_Tolerated_Dose_A = 5;
        double Max_Tolerated_Dose_B = 5;
        double Min_Drug_Dose_A = 0.5;
        double Min_Drug_Dose_B = 0.5;
        double DRUG_ON_TIME = 1;
        double DRUG_CYCLE_TIME = 24;
        double DRUG_DIFF_RATE = 2.0;
        double DRUG_UPTAKE = 1.0;
        double DRUG_A_DEATH = 0.04;
        double DRUG_B_DEATH = 0.04;

        int Check_Tumorsize_Interval_AT = 72;
        double Tumor_Size_Percent_Triggering_AT = 0.5;
        double Replacement_Threshold = 1;
        double Measurement_Noise_SD = 5;

        UILabel tickLabel = new UILabel("TICKLABEL                                                                                                 ");
        UILabel popLabel = new UILabel("POPLABEL                                                                                                   ");
        UILabel drugLabel = new UILabel("DRUG                                                                                                                             ");
        UIWindow win = new UIWindow();

        for (int i = 0; i < models.length; i++) {
//            models[i] = new PpAlternatePenultimateMultGrid(x, y, new Rand(1), Div_Prob_Sen, Div_Prob_Res, deathProb, AT_alpha_drug_A, AT_alpha_drug_B,AT_gamma_drug_A,AT_gamma_drug_B, AT_beta, Max_Tolerated_Dose_A, Max_Tolerated_Dose_B, Min_Drug_Dose_A, Min_Drug_Dose_B, DRUG_ON_TIME, DRUG_CYCLE_TIME, DRUG_DIFF_RATE, DRUG_UPTAKE, DRUG_A_DEATH, DRUG_B_DEATH, Check_Tumorsize_Interval_AT, Tumor_Size_Percent_Triggering_AT, Replacement_Threshold, Measurement_Noise_SD);//, tickLabel, popLabel, drugLabel);
            models[i] = new PpAlternatePenultimateMultGrid(x, y, new Rand(System.currentTimeMillis()), Div_Prob_Sen, Div_Prob_Res, deathProb, AT_alpha_drug_A, AT_alpha_drug_B,AT_gamma_drug_A,AT_gamma_drug_B, AT_beta, Max_Tolerated_Dose_A, Max_Tolerated_Dose_B, Min_Drug_Dose_A, Min_Drug_Dose_B, DRUG_ON_TIME, DRUG_CYCLE_TIME, DRUG_DIFF_RATE, DRUG_UPTAKE, DRUG_A_DEATH, DRUG_B_DEATH, Check_Tumorsize_Interval_AT, Tumor_Size_Percent_Triggering_AT, Replacement_Threshold, Measurement_Noise_SD,tickLabel,popLabel,drugLabel);//, tickLabel, popLabel, drugLabel);
            models[i].InitTumor(tumorRad, resistantProp);
        }
        models[0].DRUG_CYCLE_TIME = 0;//no drugA
        models[1].DRUG_CYCLE_TIME = models[1].DRUG_CYCLE_TIME;
        models[2].DRUG_CYCLE_TIME = 6; //drug every 6 hours
        double total_dose_metronomic_therapy_drugA=models[1].MAX_TOLERATED_DOSE_A*(5000/models[1].DRUG_CYCLE_TIME); //5000 ticks total
        double number_of_doses_metronomic_therapy_drugA=5000/models[2].DRUG_CYCLE_TIME;
        double unit_dose_metronomic_therapy_drugA=total_dose_metronomic_therapy_drugA/number_of_doses_metronomic_therapy_drugA;
        models[2].MAX_TOLERATED_DOSE_A = unit_dose_metronomic_therapy_drugA; //metronomic therapy keeps total drug constant as MTD

        double total_dose_metronomic_therapy_drugB=models[1].MAX_TOLERATED_DOSE_B*(5000/models[1].DRUG_CYCLE_TIME); //5000 ticks total
        double number_of_doses_metronomic_therapy_drugB=5000/models[2].DRUG_CYCLE_TIME;
        double unit_dose_metronomic_therapy_drugB=total_dose_metronomic_therapy_drugB/number_of_doses_metronomic_therapy_drugB;
        models[2].MAX_TOLERATED_DOSE_B = unit_dose_metronomic_therapy_drugB; //metronomic therapy keeps total drug constant as MTD

//        models[2].DRUG_CYCLE_TIME = models[2].DRUG_CYCLE_TIME/4; //drug every 24/4=6 hours
//        models[2].MAX_TOLERATED_DOSE_A = models[2].MAX_TOLERATED_DOSE_A/10; //metronomic therapy uses 1/10 drug
//        models[2].MAX_TOLERATED_DOSE_B = models[2].MAX_TOLERATED_DOSE_B/10; //metronomic therapy uses 1/10 drug

//        models[2].DRUG_ON_TIME = 40;
//        models[2].DRUG_CYCLE_TIME = 200;
        //constant drugA
        //Main run loop
        win.AddCol(0, tickLabel);
        win.AddCol(0, popLabel);
        win.AddCol(0, drugLabel);
        win.AddCol(0, vis);
        win.RunGui();
        for (int tick = 0; tick < 5000; tick++) {
            vis.TickPause(msPause);
            for (int i = 0; i < models.length; i++) {
                if (i == 0 || i == 1 || i == 2) {
                    models[i].ModelStep(tick);
                } else {
                    models[i].ModelStepAdaptiveTherapy(tick);
                }
                models[i].DrawModel(vis, i, tick);

            }
            //data recording
            popsOut.Write(tick+","+strategy+","+sub_strategy+","+models[0].Pop()+","+models[0].SEN_POP+","+models[0].RES_DRUG_A_POP+","+models[0].RES_DRUG_B_POP+","+models[0].RES_POP+","+models[1].Pop()+","+models[1].SEN_POP+","+models[1].RES_DRUG_A_POP+","+models[1].RES_DRUG_B_POP+","+models[1].RES_POP+","+models[2].Pop()+","+models[2].SEN_POP+","+models[2].RES_DRUG_A_POP+","+models[2].RES_DRUG_B_POP+","+models[2].RES_POP+","+models[3].Pop()+","+models[3].SEN_POP+","+models[3].RES_DRUG_A_POP+","+models[3].RES_DRUG_B_POP+","+models[3].RES_POP+","+models[3].CURRENT_DRUG_A+","+models[3].CURRENT_DRUG_B+",");
            if(!models[3].HAS_AT_STARTED) {
                if (tick%models[3].CHECK_TUMORSIZE_INTERVAL_AT==0) {
                    popsOut.Write("yes"+"\n");
                }
                else {
                    popsOut.Write("no"+"\n");
                }
            }
            else if (models[3].HAS_AT_STARTED) {
                if((tick-models[3].AT_START_TICK)%models[3].CHECK_TUMORSIZE_INTERVAL_AT==0) {
                    popsOut.Write("yes"+"\n");
                }
                else{
                    popsOut.Write("no"+"\n");


                }
            }
//            //data recording
//            if(!models[3].HAS_AT_STARTED) {
//                if ((tick-models[3].AT_START_TICK)%models[3].CHECK_TUMORSIZE_INTERVAL_AT==0) {
//                    popsOut.Write(models[0].Pop()+","+models[0].SEN_POP+","+models[0].RES_DRUG_A_POP+","+models[0].RES_DRUG_B_POP+","+models[0].RES_POP+","+models[1].Pop()+","+models[1].SEN_POP+","+models[1].RES_DRUG_A_POP+","+models[1].RES_DRUG_B_POP+","+models[1].RES_POP+","+models[2].Pop()+","+models[2].SEN_POP+","+models[2].RES_DRUG_A_POP+","+models[2].RES_DRUG_B_POP+","+models[2].RES_POP+","+models[3].Pop()+","+models[3].SEN_POP+","+models[3].RES_DRUG_A_POP+","+models[3].RES_DRUG_B_POP+","+models[3].RES_POP+","+models[3].CURRENT_DRUG_A+","+models[3].CURRENT_DRUG_B+","+tick+"\n");
//                }
//            }
//            else if (models[3].HAS_AT_STARTED && ((tick-models[3].AT_START_TICK)%models[3].CHECK_TUMORSIZE_INTERVAL_AT==0)) {
//                popsOut.Write(models[0].Pop()+","+models[0].SEN_POP+","+models[0].RES_DRUG_A_POP+","+models[0].RES_DRUG_B_POP+","+models[0].RES_POP+","+models[1].Pop()+","+models[1].SEN_POP+","+models[1].RES_DRUG_A_POP+","+models[1].RES_DRUG_B_POP+","+models[1].RES_POP+","+models[2].Pop()+","+models[2].SEN_POP+","+models[2].RES_DRUG_A_POP+","+models[2].RES_DRUG_B_POP+","+models[2].RES_POP+","+models[3].Pop()+","+models[3].SEN_POP+","+models[3].RES_DRUG_A_POP+","+models[3].RES_DRUG_B_POP+","+models[3].RES_POP+","+models[3].CURRENT_DRUG_A+","+models[3].CURRENT_DRUG_B+","+tick+"\n");
//            }
//            popsOut.Write(models[0].Pop() + "," + models[0].SEN_POP + "," + models[0].RES_DRUG_A_POP + "," + models[0].RES_DRUG_B_POP + "," + models[0].RES_POP + "," + models[1].Pop() + "," + models[1].SEN_POP + "," + models[1].RES_DRUG_A_POP + "," + models[1].RES_DRUG_B_POP + "," + models[1].RES_POP + "," + models[2].Pop() + "," + models[2].SEN_POP + "," + models[2].RES_DRUG_A_POP + "," + models[2].RES_DRUG_B_POP + "," + models[2].RES_POP + "," + models[3].Pop() + "," + models[3].SEN_POP + "," + models[3].RES_DRUG_A_POP + "," + models[3].RES_DRUG_B_POP + "," + models[3].RES_POP + "," + models[3].CURRENT_DRUG_A + "," + models[3].CURRENT_DRUG_B + "\n");
//            if((tick)%100==0) {
            //        vis.ToPNG("ModelsTick" +tick+".png");
//            }
        }
        popsOut.Close();
        win.Close();
    }

    public void InitTumor(int radius, double resistantProb) {
        //get a list of indices that fill a circle at the center of the grid
        int[] tumorNeighborhood = CircleHood(true, radius);
        int hoodSize = MapHood(tumorNeighborhood, xDim / 2, yDim / 2);
        for (int i = 0; i < hoodSize; i++) {

            int tmpIndex = rn.Int(4);
            int tmp_type;
            switch (tmpIndex) {
                case 0:
                    tmp_type = DOUBLY_RESISTANT;
                    break;
                case 1:
                    tmp_type = RESISTANT_TO_DRUG_A;
                    break;
                case 2:
                    tmp_type = RESISTANT_TO_DRUG_B;
                    break;
                case 3:
                    tmp_type = DOUBLY_SENSITIVE;
                    break;
                default:
                    throw new IllegalArgumentException("Index outside allowed range of index " + tmpIndex);
            }
//            int tmp = rn.Double() < resistantProb?rn.Double()<resistantProb?DOUBLY_RESISTANT:RESISTANT_TO_DRUG_A:rn.Double()<resistantProb?DOUBLY_SENSITIVE:RESISTANT_TO_DRUG_B;
            NewAgentSQ(tumorNeighborhood[i]).type = tmp_type;
            if (tmp_type == DOUBLY_RESISTANT) {
                RES_POP++;
            }
            if (tmp_type == DOUBLY_SENSITIVE) {
                SEN_POP++;
            }
            if (tmp_type == RESISTANT_TO_DRUG_A) {
                RES_DRUG_A_POP++;
            }
            if (tmp_type == RESISTANT_TO_DRUG_B) {
                RES_DRUG_B_POP++;
            }
        }
    }

    public void ModelStep(int tick) {

        if (!HAS_STANDARD_THERAPY_STARTED) {
            StandardTherapyChecker(tick);
        }

//        StandardTherapyChecker(tick);

        if (HAS_STANDARD_THERAPY_STARTED) {

            if (DRUG_CYCLE_ITERATOR >= DRUG_CYCLE_TIME) {
                Drug_Deliverer(DRUG_DIFF_RATE, MAX_TOLERATED_DOSE_A, MAX_TOLERATED_DOSE_B, false);
                DRUG_CYCLE_ITERATOR = 0;
            } else if (DRUG_CYCLE_ITERATOR % DRUG_CYCLE_TIME != 0) {
                Drug_Deliverer(DRUG_DIFF_RATE, MAX_TOLERATED_DOSE_A, MAX_TOLERATED_DOSE_B, false);
                DRUG_CYCLE_ITERATOR++;
            } else if (DRUG_CYCLE_ITERATOR % DRUG_CYCLE_TIME == 0) {
                Drug_Deliverer(DRUG_DIFF_RATE, MAX_TOLERATED_DOSE_A, MAX_TOLERATED_DOSE_B, true);
                DRUG_CYCLE_ITERATOR++;
            }
        }

        ShuffleAgents(rn);
        for (PpAlternatePenultimateMultCell cell : this) {
            cell.CellStep();
        }
    }

    public void ModelStepAdaptiveTherapy(int tick) {

        AdaptiveTherapyChecker(tick);

        if (HAS_AT_STARTED) {

            if (DRUG_CYCLE_ITERATOR >= CHECK_TUMORSIZE_INTERVAL_AT) {
                Drug_Deliverer(DRUG_DIFF_RATE, CURRENT_DRUG_A, CURRENT_DRUG_B, false);
                DRUG_CYCLE_ITERATOR = 0;
            } else if (DRUG_CYCLE_ITERATOR % DRUG_CYCLE_TIME != 0) {
                Drug_Deliverer(DRUG_DIFF_RATE, CURRENT_DRUG_A, CURRENT_DRUG_B, false);
                DRUG_CYCLE_ITERATOR++;
            } else if (DRUG_CYCLE_ITERATOR % DRUG_CYCLE_TIME == 0) {
                Drug_Deliverer(DRUG_DIFF_RATE, CURRENT_DRUG_A, CURRENT_DRUG_B, true);
                DRUG_CYCLE_ITERATOR++;
            }
        }

        ShuffleAgents(rn);
        for (PpAlternatePenultimateMultCell cell : this) {
            cell.CellStep();
        }
    }

    public void Drug_Deliverer(double drug_diffusion_rate, double drugA_amount, double drugB_amount, boolean is_drug_on) {

        drugA.MulAll(0.9); //first order drug decay kinetics for Tamoxifen per hour calculated after finding elimination rate constant lambda. Lambda is Clearance (CL) over Volume of Distribution (Vd).Vd is 55L/kg and CL is 2.5L/hour. Then C=C0exp(-lambda*t) where t is 1 hour
        drugB.MulAll(0.9);

        if (!IS_DRUG_FROM_PERIPHERY) {
            if (is_drug_on) {
                drugA.AddAll(drugA_amount);
                drugB.AddAll(drugB_amount);

            }
            drugA.DiffusionADI(drug_diffusion_rate);
            drugB.DiffusionADI(drug_diffusion_rate);
        } else if (IS_DRUG_FROM_PERIPHERY) {
            if (is_drug_on) {
                drugA.DiffusionADI(drug_diffusion_rate, drugA_amount);
                drugB.DiffusionADI(drug_diffusion_rate, drugB_amount);
            }
        }

    }

    public void DrawModel(UIGrid vis, int iModel, int tick) {
        tickLabel.SetText("Tick:  " + tick);
        if (tick > 200) {
            vis.TickPause(0);
            switch (iModel) {
//                case 0:
//                    popLabel.SetText("Pop No Drug is "+Pop());
//                    drugLabel.SetText("Current Drugs (A and B) (No drug case) is "+CURRENT_DRUG_A+" "+CURRENT_DRUG_B+"       Grid Avg is "+drugA.GetAvg()+" "+drugB.GetAvg());
//                    break;
//                case 1:
//                    popLabel.SetText("Pop Continuous Therapy is "+Pop());
//                    drugLabel.SetText("Current Drugs (A and B) CT is "+CURRENT_DRUG_A+" "+CURRENT_DRUG_B+"       Grid Avg is "+drugA.GetAvg()+" "+drugB.GetAvg());
//                    break;
//                case 2:
//                    popLabel.SetText("Pop Metronomic is " +Pop());
//                    drugLabel.SetText("Current Drugs (A and B) Metronomic is "+CURRENT_DRUG_A+" "+CURRENT_DRUG_B+"       Grid Avg is "+drugA.GetAvg()+" "+drugB.GetAvg());
//                    break;
                case 3:
                    popLabel.SetText("Pop Adaptive Therapy is " + Pop());
                    drugLabel.SetText("Current Drugs (A and B) AT is " + CURRENT_DRUG_A + " " + CURRENT_DRUG_B + "       Grid Avg is " + drugA.GetAvg() + " " + drugB.GetAvg());
                    break;
            }
        }
        for (int i = 0; i < length; i++) {
            PpAlternatePenultimateMultCell drawMe = GetAgent(i);
            //if the cell does not exist, draw the drug concentration
            vis.SetPix(ItoX(i) + iModel * xDim, ItoY(i), drawMe == null ? HeatMapBRG(drugA.Get(i)) : drawMe.type);
        }
//        vis.TickPause(0);
    }

    public boolean StandardTherapyChecker(int tick) {
        CURRENT_POP = Pop();
        CURRENT_POP = (int) rn.Gaussian(CURRENT_POP, MEASUREMENT_NOISE_SD);
//        System.out.println("Tumor measurement after noise is " +CURRENT_POP);
        if (!HAS_STANDARD_THERAPY_STARTED) {
            if (CURRENT_POP >= TUMOR_SIZE_TRIGGERING_AT) {
                HAS_STANDARD_THERAPY_STARTED = true;
                return true;
            }
        } else if (HAS_STANDARD_THERAPY_STARTED) {
            return true;
        }
        return false;
    }

    public boolean AdaptiveTherapyChecker(int tick) {
//        Start of Adaptive Therapy


        CURRENT_POP = Pop();
        CURRENT_POP = (int) rn.Gaussian(CURRENT_POP, MEASUREMENT_NOISE_SD);
//        System.out.println("Tumor measurement after noise is " +CURRENT_POP);


        if (!HAS_AT_STARTED) {

            if (CURRENT_POP >= TUMOR_SIZE_TRIGGERING_AT) {
                HAS_AT_STARTED = true;
                //The following ensures treatment starts with Drug A only
                CURRENT_DRUG_A = MAX_TOLERATED_DOSE_A;
                IS_IT_FIRST_TIME_DRUG_A=true;
                CURRENT_DRUG_B = 0.0;
                System.out.println("Tick: " + tick + " Current Drug A and B for first dose and Current Pop " + CURRENT_POP + " >= Tumor size triggering AT is " + CURRENT_DRUG_A + " " + CURRENT_DRUG_B + ". Prev pop is " + PREV_POP_DRUG_A);
                //The following ensures that drug B is the first drug whose dose is adjusted after initial delivery of drug A at MTD
                IS_LAST_DRUG_ADJUSTED_B = false;
                IS_LAST_DRUG_ADJUSTED_A = true;
                AT_START_TICK = tick;
                INITIAL_POP = CURRENT_POP;
                PREV_POP_DRUG_A = CURRENT_POP;
                PREV_DRUG_A = CURRENT_DRUG_A;
//                PREV_DRUG_B = MAX_TOLERATED_DOSE_B;
                IS_AT_ON = true;
                IS_TREATMENT_VACATION_ON=false;
                return true;

            }
        }

        if (HAS_AT_STARTED) {

            if ((tick - AT_START_TICK) % CHECK_TUMORSIZE_INTERVAL_AT == 0) {

                FINAL_POP = CURRENT_POP;
                VARIABLE_BETA = INITIAL_POP > FINAL_POP ? INITIAL_POP - FINAL_POP : 0;
                System.out.println("Variable Beta is " + VARIABLE_BETA);



                if (IS_LAST_DRUG_ADJUSTED_A) {

                    if(!IS_IT_FIRST_TIME_DRUG_B) {
                        CURRENT_DRUG_B=MAX_TOLERATED_DOSE_B;
                        CURRENT_DRUG_A=0.0;
                        IS_IT_FIRST_TIME_DRUG_B=true;
                        System.out.println("Tick: "+tick+" Current Drug A and B for CurrentPop "+CURRENT_POP+" is "+CURRENT_DRUG_A+" "+CURRENT_DRUG_B+". Prev pop is "+PREV_POP_DRUG_B);
                        IS_LAST_DRUG_ADJUSTED_B=true;
                        IS_LAST_DRUG_ADJUSTED_A=false;
//                        CURRENT_POP_DRUG_A=CURRENT_POP;
                        PREV_POP_DRUG_B=CURRENT_POP;
                        PREV_DRUG_B=CURRENT_DRUG_B;
//                        PREV_POP_DRUG_A=CURRENT_POP;
                        IS_AT_ON=true;
                        IS_TREATMENT_VACATION_ON=false;
                        return true;
                    }

                    if(CURRENT_POP<=(0.5*TUMOR_SIZE_TRIGGERING_AT)) {
                        CURRENT_DRUG_A=0.0;
                        CURRENT_DRUG_B=0.0;
                        System.out.println("Tick: "+tick+" Current Drug A and B for CurrentPop "+CURRENT_POP+" <=0.5 of the tumor size triggering AT is "+CURRENT_DRUG_A+" "+CURRENT_DRUG_B+". Prev pop is "+PREV_POP_DRUG_B);
                        PREV_POP_DRUG_B=CURRENT_POP;
                        if (!IS_TREATMENT_VACATION_ON) {
//                            PREV_DRUG_A=(0.5*PREV_DRUG_A<=MIN_DRUG_DOSE_A)?MIN_DRUG_DOSE_A:0.5*PREV_DRUG_A;
                            PREV_DRUG_B=(0.5*PREV_DRUG_B<=MIN_DRUG_DOSE_B)?MIN_DRUG_DOSE_B:0.5*PREV_DRUG_B;
                            IS_TREATMENT_VACATION_ON=true;
                        }
                        else if (IS_TREATMENT_VACATION_ON) {
//                            PREV_DRUG_A=PREV_DRUG_A;
                            PREV_DRUG_B=PREV_DRUG_B;
                        }
                        IS_AT_ON=true;
                        IS_LAST_DRUG_ADJUSTED_A=false;
                        IS_LAST_DRUG_ADJUSTED_B=true;
                        return true;
                    }
                    if (CURRENT_POP > (int) (0.95 * length)) {
                        CURRENT_DRUG_B = MAX_TOLERATED_DOSE_B;
                        CURRENT_DRUG_A = 0.0;
                        System.out.println("Tick: " + tick + " Current Drug A and B for pop " + CURRENT_POP + " >0.95*carrying capacity(xDim*yDim)  " + CURRENT_DRUG_A + " " + CURRENT_DRUG_B + ". Prev pop is " + PREV_POP_DRUG_B);
                        PREV_POP_DRUG_B = CURRENT_POP;
                        PREV_DRUG_B = CURRENT_DRUG_B;
                        IS_AT_ON = true;
                        IS_LAST_DRUG_ADJUSTED_A = false;
                        IS_LAST_DRUG_ADJUSTED_B = true;
                        IS_TREATMENT_VACATION_ON=false;
                        return true;
                    }

                    if (PREV_POP_DRUG_A > ((int) (((1 + AT_BETA) * PREV_POP_DRUG_B)))) {
//                        CURRENT_DRUG_B = ((1 + AT_ALPHA_DRUG_B) * PREV_DRUG_B) >= MAX_TOLERATED_DOSE_B ? MAX_TOLERATED_DOSE_B : ((1 + AT_ALPHA_DRUG_B) * PREV_DRUG_B);
                        CURRENT_DRUG_B=(AT_GAMMA_DRUG_B*PREV_DRUG_B)>=MAX_TOLERATED_DOSE_B?MAX_TOLERATED_DOSE_B:((AT_GAMMA_DRUG_B)*PREV_DRUG_B);
                        CURRENT_DRUG_A = 0.0;
                        System.out.println("Tick: " + tick + " Current Drug A and B for CurrentPop " + CURRENT_POP + " >(1+at_beta)*Prev Pop " + PREV_POP_DRUG_B + " is " + CURRENT_DRUG_A + " " + CURRENT_DRUG_B);
                        PREV_POP_DRUG_B = CURRENT_POP;
//                        CURRENT_POP_DRUG_A=CURRENT_POP;
                        PREV_DRUG_B = CURRENT_DRUG_B;
                        IS_AT_ON = true;
                        IS_LAST_DRUG_ADJUSTED_B = true;
                        IS_LAST_DRUG_ADJUSTED_A = false;
                        IS_TREATMENT_VACATION_ON=false;
                        return true;
                    }

                    if (PREV_POP_DRUG_A <= ((int) (((1 - AT_BETA) * PREV_POP_DRUG_B)))) {
//                        CURRENT_DRUG_B = ((1 - AT_ALPHA_DRUG_B) * PREV_DRUG_B) <= MIN_DRUG_DOSE_B ? MIN_DRUG_DOSE_B : (1 - AT_ALPHA_DRUG_B) * PREV_DRUG_B;
                        CURRENT_DRUG_B=(PREV_DRUG_B/AT_GAMMA_DRUG_B)<=MIN_DRUG_DOSE_B?MIN_DRUG_DOSE_B:(PREV_DRUG_B/AT_GAMMA_DRUG_B);
                        CURRENT_DRUG_A = 0.0;
                        System.out.println("Tick: " + tick + " Current Drug A and B for CurrentPop " + CURRENT_POP + " <=(1-at_beta)*Prev Pop " + PREV_POP_DRUG_B + " is " + CURRENT_DRUG_A + " " + CURRENT_DRUG_B);
                        PREV_POP_DRUG_B = CURRENT_POP;
//                        CURRENT_POP_DRUG_A=CURRENT_POP;
                        PREV_DRUG_B = CURRENT_DRUG_B;
                        IS_AT_ON = true;
                        IS_LAST_DRUG_ADJUSTED_B = true;
                        IS_LAST_DRUG_ADJUSTED_A = false;
                        IS_TREATMENT_VACATION_ON=false;
                        return true;
                    }

                    if (PREV_POP_DRUG_A > ((int) ((1 - AT_BETA) * PREV_POP_DRUG_B)) && PREV_POP_DRUG_A <= ((int) ((1 + AT_BETA) * PREV_POP_DRUG_B))) {
                        if (VARIABLE_BETA > 0 && CURRENT_POP < INITIAL_POP) {
                            CURRENT_DRUG_B = PREV_DRUG_B;
                            CURRENT_DRUG_A = 0.0;
                            System.out.println("Tick: " + tick + " Current Drug A and B for Current Pop " + CURRENT_POP + " within the threshold(beta) of prev pop " + PREV_POP_DRUG_B + " is " + CURRENT_DRUG_A + " " + CURRENT_DRUG_B);
                            PREV_POP_DRUG_B = CURRENT_POP;
//                            CURRENT_POP_DRUG_A=CURRENT_POP;
                            PREV_DRUG_B = CURRENT_DRUG_B;
                            IS_AT_ON = true;
                            IS_LAST_DRUG_ADJUSTED_A = false;
                            IS_LAST_DRUG_ADJUSTED_B = true;
                            IS_TREATMENT_VACATION_ON=false;
                            return true;
                        } else {
//                            CURRENT_DRUG_B = ((1 + AT_ALPHA_DRUG_B) * PREV_DRUG_B) >= MAX_TOLERATED_DOSE_B ? MAX_TOLERATED_DOSE_B : ((1 + AT_ALPHA_DRUG_B) * PREV_DRUG_B);
                            CURRENT_DRUG_B=(AT_GAMMA_DRUG_B*PREV_DRUG_B)>=MAX_TOLERATED_DOSE_B?MAX_TOLERATED_DOSE_B:((AT_GAMMA_DRUG_B)*PREV_DRUG_B);
                            CURRENT_DRUG_A = 0.0;
                            System.out.println("Tick: " + tick + " Current Drug A and B for CurrentPop " + CURRENT_POP + " >(1+at_beta)*Prev Pop " + PREV_POP_DRUG_B + " is " + CURRENT_DRUG_A + " " + CURRENT_DRUG_B);
                            PREV_POP_DRUG_B = CURRENT_POP;
//                            PREV_DRUG_A=CURRENT_DRUG_A;
                            PREV_DRUG_B = CURRENT_DRUG_B;
                            IS_AT_ON = true;
                            IS_LAST_DRUG_ADJUSTED_B = true;
                            IS_LAST_DRUG_ADJUSTED_A = false;
                            IS_TREATMENT_VACATION_ON = false;
                            INITIAL_POP = CURRENT_POP;
                            return true;
                        }
                    }
                }

                if (IS_LAST_DRUG_ADJUSTED_B) {

                    if(CURRENT_POP<=(0.5*TUMOR_SIZE_TRIGGERING_AT)) {
                        CURRENT_DRUG_A=0.0;
                        CURRENT_DRUG_B=0.0;
                        System.out.println("Tick: "+tick+" Current Drug A and B for CurrentPop "+CURRENT_POP+" <=0.5 of the tumor size triggering AT is "+CURRENT_DRUG_A+" "+CURRENT_DRUG_B+". Prev pop is "+PREV_POP_DRUG_A);
                        PREV_POP_DRUG_A=CURRENT_POP;
                        if (!IS_TREATMENT_VACATION_ON) {
                            PREV_DRUG_A=(0.5*PREV_DRUG_A<=MIN_DRUG_DOSE_A)?MIN_DRUG_DOSE_A:0.5*PREV_DRUG_A;
//                            PREV_DRUG_B=(0.5*PREV_DRUG_B<=MIN_DRUG_DOSE_B)?MIN_DRUG_DOSE_B:0.5*PREV_DRUG_B;
                            IS_TREATMENT_VACATION_ON=true;
                        }
                        else if (IS_TREATMENT_VACATION_ON) {
                            PREV_DRUG_A=PREV_DRUG_A;
//                            PREV_DRUG_B=PREV_DRUG_B;
                        }
                        IS_AT_ON=true;
                        IS_LAST_DRUG_ADJUSTED_A=true;
                        IS_LAST_DRUG_ADJUSTED_B=false;
                        return true;
                    }
                    if (CURRENT_POP > (int) (0.95 * length)) {
                        CURRENT_DRUG_A = MAX_TOLERATED_DOSE_A;
                        CURRENT_DRUG_B = 0.0;
                        System.out.println("Tick: " + tick + " Current Drug A and B for pop " + CURRENT_POP + " >0.95*carrying capacity(xDim*yDim)  " + CURRENT_DRUG_A + " " + CURRENT_DRUG_B + ". Prev pop is " + PREV_POP_DRUG_A);
                        PREV_POP_DRUG_A = CURRENT_POP;
                        PREV_DRUG_A = CURRENT_DRUG_A;
                        IS_AT_ON = true;
                        IS_LAST_DRUG_ADJUSTED_A = true;
                        IS_LAST_DRUG_ADJUSTED_B = false;
                        IS_TREATMENT_VACATION_ON=false;
                        return true;
                    }
                    if (PREV_POP_DRUG_B > ((int) (((1 + AT_BETA) * PREV_POP_DRUG_A)))) {
//                        CURRENT_DRUG_A = ((1 + AT_ALPHA_DRUG_A) * PREV_DRUG_A) >= MAX_TOLERATED_DOSE_A ? MAX_TOLERATED_DOSE_A : ((1 + AT_ALPHA_DRUG_A) * PREV_DRUG_A);
                        CURRENT_DRUG_A=(AT_GAMMA_DRUG_A*PREV_DRUG_A)>=MAX_TOLERATED_DOSE_A?MAX_TOLERATED_DOSE_A:((AT_GAMMA_DRUG_A)*PREV_DRUG_A);
                        CURRENT_DRUG_B = 0.0;
                        System.out.println("Tick: " + tick + " Current Drug A and B for CurrentPop " + CURRENT_POP + " >(1+at_beta)*Prev Pop " + PREV_POP_DRUG_A + " is " + CURRENT_DRUG_A + " " + CURRENT_DRUG_B);
                        PREV_POP_DRUG_A = CURRENT_POP;
//                        CURRENT_POP_DRUG_B=CURRENT_POP;
                        PREV_DRUG_A = CURRENT_DRUG_A;
                        IS_AT_ON = true;
                        IS_LAST_DRUG_ADJUSTED_A = true;
                        IS_LAST_DRUG_ADJUSTED_B = false;
                        IS_TREATMENT_VACATION_ON=false;
                        return true;
                    }

                    if (PREV_POP_DRUG_B <= ((int) (((1 - AT_BETA) * PREV_POP_DRUG_A)))) {
//                        CURRENT_DRUG_A = ((1 - AT_ALPHA_DRUG_A) * PREV_DRUG_A) <= MIN_DRUG_DOSE_A ? MIN_DRUG_DOSE_A : (1 - AT_ALPHA_DRUG_A) * PREV_DRUG_A;
                        CURRENT_DRUG_A=(PREV_DRUG_A/AT_GAMMA_DRUG_A)<=MIN_DRUG_DOSE_A?MIN_DRUG_DOSE_A:(PREV_DRUG_A/AT_GAMMA_DRUG_A);
                        CURRENT_DRUG_B = 0.0;
                        System.out.println("Tick: " + tick + " Current Drug A and B for CurrentPop " + CURRENT_POP + " <=(1-at_beta)*Prev Pop " + PREV_POP_DRUG_A + " is " + CURRENT_DRUG_A + " " + CURRENT_DRUG_B);
                        PREV_POP_DRUG_A = CURRENT_POP;
//                        CURRENT_POP_DRUG_B=CURRENT_POP;
                        PREV_DRUG_A = CURRENT_DRUG_A;
                        IS_AT_ON = true;
                        IS_LAST_DRUG_ADJUSTED_A = true;
                        IS_LAST_DRUG_ADJUSTED_B = false;
                        IS_TREATMENT_VACATION_ON=false;
                        return true;
                    }

                    if (PREV_POP_DRUG_B > ((int) ((1 - AT_BETA) * PREV_POP_DRUG_A)) && PREV_POP_DRUG_B <= ((int) ((1 + AT_BETA) * PREV_POP_DRUG_A))) {
                        if (VARIABLE_BETA > 0 && CURRENT_POP < INITIAL_POP) {
                            CURRENT_DRUG_A = PREV_DRUG_A;
                            CURRENT_DRUG_B = 0.0;
                            System.out.println("Tick: " + tick + " Current Drug A and B for Current Pop " + CURRENT_POP + " within the threshold(beta) of prev pop " + PREV_POP_DRUG_A + " is " + CURRENT_DRUG_A + " " + CURRENT_DRUG_B);
                            PREV_POP_DRUG_A = CURRENT_POP;
//                            CURRENT_POP_DRUG_B=CURRENT_POP;
                            PREV_DRUG_A = CURRENT_DRUG_A;
                            IS_AT_ON = true;
                            IS_LAST_DRUG_ADJUSTED_A = true;
                            IS_LAST_DRUG_ADJUSTED_B = false;
                            IS_TREATMENT_VACATION_ON=false;
                            return true;
                        } else {
//                            CURRENT_DRUG_A = ((1 + AT_ALPHA_DRUG_A) * PREV_DRUG_A) >= MAX_TOLERATED_DOSE_A ? MAX_TOLERATED_DOSE_A : ((1 + AT_ALPHA_DRUG_A) * PREV_DRUG_A);
                            CURRENT_DRUG_A=(AT_GAMMA_DRUG_A*PREV_DRUG_A)>=MAX_TOLERATED_DOSE_A?MAX_TOLERATED_DOSE_A:((AT_GAMMA_DRUG_A)*PREV_DRUG_A);
                            CURRENT_DRUG_B = 0.0;
                            System.out.println("Tick: " + tick + " Current Drug A and B for CurrentPop " + CURRENT_POP + " >(1+at_beta)*Prev Pop " + PREV_POP_DRUG_A + " is " + CURRENT_DRUG_A + " " + CURRENT_DRUG_B);
                            PREV_POP_DRUG_A = CURRENT_POP;
                            PREV_DRUG_A = CURRENT_DRUG_A;
//                            PREV_DRUG_B=CURRENT_DRUG_B;
                            IS_AT_ON = true;
                            IS_LAST_DRUG_ADJUSTED_A = true;
                            IS_LAST_DRUG_ADJUSTED_B = false;
                            IS_TREATMENT_VACATION_ON = false;
                            INITIAL_POP = CURRENT_POP;
                            return true;
                        }
                    }
                }
                return false;
            }
        }
        return false;
    }
}

class PpAlternatePenultimateMultCell extends AgentSQ2Dunstackable<PpAlternatePenultimateMultGrid> {

//    public double generation_time;
//    public double drug_sensitivity_score;
    public int type;

    public void Mutate() {
        if(type==G.DOUBLY_SENSITIVE) {
            int tmp=G.rn.Int(3);
            switch (tmp) {
                case 0:
                    if (G.rn.Double(1)<=G.Mut_DoublySen_to_ResA) {
                        type=G.RESISTANT_TO_DRUG_A;
                        G.SEN_POP--;
                        G.RES_DRUG_A_POP++;
                    }
                    break;
                case 1:
                    if (G.rn.Double(1)<=G.Mut_DoublySen_to_ResB) {
                        type=G.RESISTANT_TO_DRUG_B;
                        G.SEN_POP--;
                        G.RES_DRUG_B_POP++;
                    }
                    break;
                case 2:
                    if (G.rn.Double(1)<=G.Mut_DoublySen_to_DoublyRes) {
                        type=G.DOUBLY_RESISTANT;
                        G.SEN_POP--;
                        G.RES_POP++;
                    }
                    break;
            }
        }
        else if(type==G.DOUBLY_RESISTANT) {
            int tmp=G.rn.Int(3);
            switch (tmp) {
                case 0:
                    if (G.rn.Double(1)<=G.Mut_DoublyRes_to_ResA) {
                        type=G.RESISTANT_TO_DRUG_A;
                        G.RES_POP--;
                        G.RES_DRUG_A_POP++;
                    }
                    break;
                case 1:
                    if (G.rn.Double(1)<=G.Mut_DoublyRes_to_ResB) {
                        type=G.RESISTANT_TO_DRUG_B;
                        G.RES_POP--;
                        G.RES_DRUG_B_POP++;
                    }
                    break;
                case 2:
                    if (G.rn.Double(1)<=G.Mut_DoublyRes_to_DoublySen) {
                        type=G.DOUBLY_SENSITIVE;
                        G.RES_POP--;
                        G.SEN_POP++;
                    }
                    break;
            }
        }
        else if(type==G.RESISTANT_TO_DRUG_A) {
            int tmp=G.rn.Int(2);
            switch (tmp) {
                case 0:
                    if (G.rn.Double(1)<=G.Mut_ResA_to_DoublyRes) {
                        type=G.DOUBLY_RESISTANT;
                        G.RES_DRUG_A_POP--;
                        G.RES_POP++;
                    }
                    break;
                case 1:
                    if (G.rn.Double(1)<=G.Mut_ResA_to_DoublySen) {
                        type=G.DOUBLY_SENSITIVE;
                        G.RES_DRUG_A_POP--;
                        G.SEN_POP++;
                    }
                    break;
            }
        }
        else if(type==G.RESISTANT_TO_DRUG_B) {
            int tmp=G.rn.Int(2);
            switch (tmp) {
                case 0:
                    if (G.rn.Double(1)<=G.Mut_ResB_to_DoublyRes) {
                        type=G.DOUBLY_RESISTANT;
                        G.RES_DRUG_B_POP--;
                        G.RES_POP++;
                    }
                    break;
                case 1:
                    if (G.rn.Double(1)<=G.Mut_ResB_to_DoublySen) {
                        type=G.DOUBLY_SENSITIVE;
                        G.RES_DRUG_B_POP--;
                        G.SEN_POP++;
                    }
                    break;
            }
        }
    }

    public void DivideAndMutate(int index) {
        PpAlternatePenultimateMultCell tmp = G.NewAgentSQ(index);

        tmp.type=this.type;
        if(this.type==G.DOUBLY_SENSITIVE) {
            G.SEN_POP++;
        }
        if(this.type==G.DOUBLY_RESISTANT) {
            G.RES_POP++;
        }
        if(this.type==G.RESISTANT_TO_DRUG_A) {
            G.RES_DRUG_A_POP++;
        }
        if(this.type==G.RESISTANT_TO_DRUG_B) {
            G.RES_DRUG_B_POP++;
        }
        this.Mutate();
        tmp.Mutate();
    }


    public void CellStep() {

//        Mutate();

        //Consumption of Drug
        G.drugA.Mul(Isq(), G.DRUG_UPTAKE);
        G.drugB.Mul(Isq(), G.DRUG_UPTAKE);
        //Chance of Death, depends on resistance and drugA concentration
        double tmp_random = G.rn.Double();
        if(type==G.DOUBLY_SENSITIVE && tmp_random<(G.DEATH_PROB+G.drugA.Get(Isq())*G.DRUG_A_DEATH+G.drugB.Get(Isq())*G.DRUG_B_DEATH)) {
            Dispose();
            G.SEN_POP--;
            return;
        }
        if(type==G.DOUBLY_RESISTANT && tmp_random<G.DEATH_PROB) {
            Dispose();
            G.RES_POP--;
            return;
        }
        if(type==G.RESISTANT_TO_DRUG_A && tmp_random<(G.DEATH_PROB+G.drugA.Get(Isq())*G.DRUG_A_DEATH)) {
            Dispose();
            G.RES_DRUG_A_POP--;
            return;
        }
        if(type==G.RESISTANT_TO_DRUG_B && tmp_random<(G.DEATH_PROB+G.drugB.Get(Isq())*G.DRUG_B_DEATH)) {
            Dispose();
            G.RES_DRUG_B_POP--;
            return;
        }


        //Chance of Division, depends on resistance
        double tmp_div_prob;
        if (this.type==G.DOUBLY_SENSITIVE) {
            tmp_div_prob=G.DIV_PROB_SEN;
        }
        else if (this.type==G.DOUBLY_RESISTANT) {
            tmp_div_prob=G.DIV_PROB_RES;
        }
        else if (this.type==G.RESISTANT_TO_DRUG_A) {
            tmp_div_prob=G.DIV_PROB_RES_DRUG_A;
        }
        else if (this.type==G.RESISTANT_TO_DRUG_B) {
            tmp_div_prob=G.DIV_PROB_RES_DRUG_B;
        } else {
            throw new IllegalArgumentException("The cell type does not match to any of the existing types");
        }


        if (G.rn.Double() < tmp_div_prob) {

            int options_empty=MapEmptyHood(G.divHood);
            if(options_empty>0){
                int myTmpIndex=G.divHood[G.rn.Int(options_empty)];
                DivideAndMutate(myTmpIndex);
            }

            if (options_empty==0 && G.rn.Double()<G.REPLACEMENT_THRESHOLD) { //&& this.type==G.DOUBLY_RESISTANT) {

                int options_occupied=MapOccupiedHood(G.divHood);
//                System.out.println("options_occupied is " + options_occupied);
                int tmpAgentIndex=G.divHood[G.rn.Int(options_occupied)];
//                System.out.println("tmpAgentIndex is " + tmpAgentIndex);
                PpAlternatePenultimateMultCell tmpAgent = G.GetAgent(tmpAgentIndex);
                int tmpAgentType = tmpAgent.type;

                tmpAgent.Dispose();
                if(tmpAgentType==G.DOUBLY_SENSITIVE) {
                    G.SEN_POP--;
                }
                if(tmpAgentType==G.DOUBLY_RESISTANT) {
                    G.RES_POP--;
                }
                if(tmpAgentType==G.RESISTANT_TO_DRUG_A) {
                    G.RES_DRUG_A_POP--;
                }
                if(tmpAgentType==G.RESISTANT_TO_DRUG_B) {
                    G.RES_DRUG_B_POP--;
                }
                DivideAndMutate(tmpAgentIndex);



            }

        }
//        Mutate();
    }

}