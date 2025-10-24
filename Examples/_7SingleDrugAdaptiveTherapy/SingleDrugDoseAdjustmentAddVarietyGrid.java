package Examples._7SingleDrugAdaptiveTherapy;

//This script simulates adaptive therapy with replacement and tumor measurement error
// Dividing cells (sensitive or resistant) can replace neighboring cells with 0 being no replacement and 1 being replacement at all times
//Resistance is binary phenotype (0 or 1)
//This script is for single drug adaptive therapy (Tamoxifen)
//17 October 2019

import Framework.GridsAndAgents.*;
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

public class SingleDrugDoseAdjustmentAddVarietyGrid extends AgentGrid2D<SingleDrugDoseAdjustmentAddVarietyCell> {
    //model constants
    public final static int RESISTANT = RGB(1, 0, 0), SENSITIVE = RGB(0, 1, 0);
    public double DIV_PROB_SEN, DIV_PROB_RES, DEATH_PROB, PREV_DRUG, CURRENT_DRUG;
    public int PREV_POP, CURRENT_POP,INITIAL_POP, FINAL_POP;
    public int SEN_POP, RES_POP;
    public int DRUG_CYCLE_ITERATOR = 0;
    public int AT_START_TICK;
    public int VARIABLE_BETA;
    public double DRUG_ON_TIME , DRUG_CYCLE_TIME, DRUG_DIFF_RATE, DRUG_UPTAKE, DRUG_DEATH;
    public double AT_ALPHA, AT_BETA, AT_GAMMA, MAX_TOLERATED_DOSE, MIN_DRUG_DOSE;
    public double REPLACEMENT_THRESHOLD;
    public double MEASUREMENT_NOISE_SD;
    public double Mut_SenToRes;
    public double Mut_ResToSen;
    //internal model objects
    public PDEGrid2D drug;
    public Rand rn;
    public int[] divHood = MooreHood(false);
    public boolean HAS_STANDARD_THERAPY_STARTED = false;
    public boolean HAS_AT_STARTED;
    public int CHECK_TUMORSIZE_INTERVAL_AT;
    public int TUMOR_SIZE_TRIGGERING_AT;
    public boolean IS_TREATMENT_VACATION_ON;
    public boolean IS_DRUG_FROM_PERIPHERY=false;//default is drug is added internally at all grids and to make drug from periphery set the boolean to true
    UILabel tickLabel;
    UILabel popLabel;
    UILabel drugLabel;


    public SingleDrugDoseAdjustmentAddVarietyGrid(int xDim, int yDim, Rand rn, double birthSensitiveProb, double birthResistantProb, double deathProb, double AT_Alpha, double AT_Gamma, double AT_Beta, double Max_Tolerated_dose, double Min_Drug_Dose,double DRUG_ON_TIME, double DRUG_CYCLE_TIME ,double DRUG_DIFF_RATE,double DRUG_UPTAKE,double DRUG_DEATH,int check_tumorsize_interval_AT, double tumor_size_percent_triggering_AT, double replacement_threshold, double measurement_noise_sd, double Mut_SenToRes, double Mut_ResToSen, UILabel tickLabel,UILabel popLabel,UILabel drugLabel) {
        super(xDim, yDim, SingleDrugDoseAdjustmentAddVarietyCell.class);
        this.rn = rn;
        this.DIV_PROB_SEN = birthSensitiveProb;
        this.DIV_PROB_RES = birthResistantProb;
        this.DEATH_PROB = deathProb;
        this.AT_ALPHA = AT_Alpha;
        this.AT_GAMMA = AT_Gamma;
        this.AT_BETA = AT_Beta;
        this.MAX_TOLERATED_DOSE = Max_Tolerated_dose;
        this.MIN_DRUG_DOSE = Min_Drug_Dose;
        this.DRUG_ON_TIME = DRUG_ON_TIME;
        this.DRUG_CYCLE_TIME = DRUG_CYCLE_TIME;
        this.DRUG_DIFF_RATE = DRUG_DIFF_RATE;
        this.DRUG_UPTAKE = DRUG_UPTAKE;
        this.DRUG_DEATH = DRUG_DEATH;
        this.HAS_AT_STARTED = false;
        this.IS_TREATMENT_VACATION_ON = false;
        this.CHECK_TUMORSIZE_INTERVAL_AT = check_tumorsize_interval_AT;
        this.TUMOR_SIZE_TRIGGERING_AT = (int)(tumor_size_percent_triggering_AT*length);
        this.REPLACEMENT_THRESHOLD = replacement_threshold;
        this.MEASUREMENT_NOISE_SD = measurement_noise_sd;
        this.Mut_SenToRes = Mut_SenToRes;
        this.Mut_ResToSen = Mut_ResToSen;
        this.tickLabel = tickLabel;
        this.popLabel = popLabel;
        this.drugLabel = drugLabel;
        drug = new PDEGrid2D(xDim, yDim);
    }

    public static void main(String[] args) {
        int x = 100, y = 100, visScale = 3, msPause = 0;
        double tumorRad = 10;
        double resistantProb = 0.5;
        UIGrid vis = new UIGrid( x*4, y, visScale,true);
        SingleDrugDoseAdjustmentAddVarietyGrid[] models = new SingleDrugDoseAdjustmentAddVarietyGrid[4];
//        String strategy = args[0];
//        String sub_strategy = args[1];
//        String file_counter = args[2];
        String strategy = "DosAdjAdd25";
        String sub_strategy = "CkTan";
        String file_counter = "1";
//        FileIO popsOut=new FileIO("DosMod2.csv","w");
        FileIO popsOut=new FileIO(strategy+"-"+sub_strategy+"-"+file_counter,"w");
//        BufferedReader userInput=new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Please enter in order: 1.Birth Prob Sensitive 2.Birth Prob Resistant 3. Death Prob 4.Alpha Param for AT 5.Beta Param for AT 6.Maximum Tolerated Dose 7. TimeSteps after which tumor size(cell number) is checked for AT 8. AT starts after what fraction of the grid is occupied pressing enter after each");
        double Div_Prob_Sen=0.08;
        double Div_Prob_Res=0.02;
        double deathProb=0.01;
        double AT_alpha=0.5;
//        double AT_alpha=Double.parseDouble(args[3]);
        double AT_gamma=0;
        double AT_beta=0.1;
        double Max_Tolerated_Dose=5;
        double Min_Drug_Dose=0.5;
        double DRUG_ON_TIME = 1;
        double DRUG_CYCLE_TIME = 24;
        double DRUG_DIFF_RATE = 2.0;
        double DRUG_UPTAKE = 1.0;
        double DRUG_DEATH = 0.04;
        int Check_Tumorsize_Interval_AT=72;
        double Tumor_Size_Percent_Triggering_AT=0.5;
        double Replacement_Threshold=1.0;
        double Measurement_Noise_SD=5;
        double Mut_SenToRes=1e-4;
        double Mut_ResToSen=0.0;
        UILabel tickLabel = new UILabel("TICKLABEL                                          ");
        UILabel popLabel = new UILabel("POPLABEL                                            ");
        UILabel drugLabel = new UILabel("DRUG                                                                                                                           ");
        UIWindow win = new UIWindow();

        for (int i = 0; i < models.length; i++) {
            models[i]=new SingleDrugDoseAdjustmentAddVarietyGrid(x,y,new Rand(1),Div_Prob_Sen,Div_Prob_Res,deathProb,AT_alpha,AT_gamma, AT_beta,Max_Tolerated_Dose, Min_Drug_Dose,DRUG_ON_TIME,DRUG_CYCLE_TIME,DRUG_DIFF_RATE,DRUG_UPTAKE,DRUG_DEATH,Check_Tumorsize_Interval_AT,Tumor_Size_Percent_Triggering_AT,Replacement_Threshold,Measurement_Noise_SD,Mut_SenToRes,Mut_ResToSen,tickLabel,popLabel,drugLabel);
            models[i].InitTumor(tumorRad, resistantProb);
        }
        models[0].DRUG_CYCLE_TIME =0;//no drug
        models[1].DRUG_CYCLE_TIME=models[1].DRUG_CYCLE_TIME;
        models[2].DRUG_CYCLE_TIME = 6; //drug every 6 hours
        double total_dose_metronomic_therapy=models[1].MAX_TOLERATED_DOSE*(5000/models[1].DRUG_CYCLE_TIME); //5000 ticks total
        double number_of_doses_metronomic_therapy=5000/models[2].DRUG_CYCLE_TIME;
        double unit_dose_metronomic_therapy=total_dose_metronomic_therapy/number_of_doses_metronomic_therapy;
        models[2].MAX_TOLERATED_DOSE = unit_dose_metronomic_therapy; //metronomic therapy keeps total drug constant as MTD
//        models[2].MAX_TOLERATED_DOSE=Math.min(0.2,Max_Tolerated_Dose/10);
//        models[2].DRUG_CYCLE_TIME=4.0;
        //constant drug
        //Main run loop
        win.AddCol(0,tickLabel);
        win.AddCol(0,popLabel);
        win.AddCol(0,drugLabel);
        win.AddCol(0,vis);
        win.RunGui();
        for (int tick = 0; tick < 5000; tick++) {
            vis.TickPause(msPause);
            for (int i = 0; i < models.length; i++) {
                if (i==0||i==1||i==2) {
                    models[i].ModelStep(tick);
                } else {
                    models[i].ModelStepAdaptiveTherapy(tick);
                }
                models[i].DrawModel(vis,i,tick);

            }

            //data recording
            popsOut.Write(tick+","+strategy+","+sub_strategy+","+models[0].Pop()+","+models[0].SEN_POP+","+models[0].RES_POP+","+models[1].Pop()+","+models[1].SEN_POP+","+models[1].RES_POP+","+models[2].Pop()+","+models[2].SEN_POP+","+models[2].RES_POP+","+models[3].Pop()+","+models[3].SEN_POP+","+models[3].RES_POP+","+models[3].CURRENT_DRUG+",");
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
//            popsOut.Write(models[0].Pop()+","+models[0].SEN_POP+","+models[0].RES_POP+","+models[1].Pop()+","+models[1].SEN_POP+","+models[1].RES_POP+","+models[2].Pop()+","+models[2].SEN_POP+","+models[2].RES_POP+","+models[3].Pop()+","+models[3].SEN_POP+","+models[3].RES_POP+","+models[3].CURRENT_DRUG+",");
//            if(!models[3].HAS_AT_STARTED) {
//                if (tick%models[3].CHECK_TUMORSIZE_INTERVAL_AT==0) {
//                    popsOut.Write(tick+","+"yes"+"\n");
//                }
//                else {
//                    popsOut.Write(tick+","+"no"+"\n");
//                }
//            }
//            else if (models[3].HAS_AT_STARTED) {
//                if((tick-models[3].AT_START_TICK)%models[3].CHECK_TUMORSIZE_INTERVAL_AT==0) {
//                    popsOut.Write(tick+","+"yes"+"\n");
//                }
//                else{
//                    popsOut.Write(tick+","+"no"+"\n");
//
//
//                }
//            }
//            //data recording
//            if(!models[3].HAS_AT_STARTED) {
//                if ((tick-models[3].AT_START_TICK)%models[3].CHECK_TUMORSIZE_INTERVAL_AT==0) {
//                    popsOut.Write(models[0].Pop()+","+models[0].SEN_POP+","+models[0].RES_POP+","+models[1].Pop()+","+models[1].SEN_POP+","+models[1].RES_POP+","+models[2].Pop()+","+models[2].SEN_POP+","+models[2].RES_POP+","+models[3].Pop()+","+models[3].SEN_POP+","+models[3].RES_POP+","+models[3].CURRENT_DRUG+","+tick+"\n");
//                }
//            }
//            else if (models[3].HAS_AT_STARTED && ((tick-models[3].AT_START_TICK)%models[3].CHECK_TUMORSIZE_INTERVAL_AT==0)) {
//                popsOut.Write(models[0].Pop()+","+models[0].SEN_POP+","+models[0].RES_POP+","+models[1].Pop()+","+models[1].SEN_POP+","+models[1].RES_POP+","+models[2].Pop()+","+models[2].SEN_POP+","+models[2].RES_POP+","+models[3].Pop()+","+models[3].SEN_POP+","+models[3].RES_POP+","+models[3].CURRENT_DRUG+","+tick+"\n");
//            }
//            popsOut.Write(models[0].Pop()+","+models[0].SEN_POP+","+models[0].RES_POP+","+models[1].Pop()+","+models[1].SEN_POP+","+models[1].RES_POP+","+models[2].Pop()+","+models[2].SEN_POP+","+models[2].RES_POP+","+models[3].Pop()+","+models[3].SEN_POP+","+models[3].RES_POP+","+models[3].CURRENT_DRUG+"\n");
//            if((tick)%100==0) {
            //        vis.ToPNG("ModelsTick" +tick+".png");
//            }
        }
        popsOut.Close();
        win.Close();
    }

    public void InitTumor(double radius, double resistantProb) {
        //get a list of indices that fill a circle at the center of the grid
        int[] tumorNeighborhood = CircleHood(true, radius);
        int hoodSize=MapHood(tumorNeighborhood,xDim/2,yDim/2);
        for (int i = 0; i < hoodSize; i++) {

            int tmpIndex=rn.Int(2);
            int tmp_type;
            switch (tmpIndex) {
                case 0:
                    tmp_type=RESISTANT;
                    break;

                case 1:
                    tmp_type=SENSITIVE;
                    break;
                default:
                    throw new IllegalArgumentException("Index outside allowed range of index " + tmpIndex);
            }
            NewAgentSQ(tumorNeighborhood[i]).type = tmp_type;
            if(tmp_type==RESISTANT) {
                RES_POP++;
            }
            if(tmp_type==SENSITIVE) {
                SEN_POP++;
            }
        }
    }

    public void ModelStep(int tick) {

        if (!HAS_STANDARD_THERAPY_STARTED) {
            StandardTherapyChecker(tick);
        }

        if (HAS_STANDARD_THERAPY_STARTED) {

            if(DRUG_CYCLE_ITERATOR>=DRUG_CYCLE_TIME) {
                Drug_Deliverer(DRUG_DIFF_RATE,MAX_TOLERATED_DOSE,false);
                DRUG_CYCLE_ITERATOR=0;
            }

            else if(DRUG_CYCLE_ITERATOR%DRUG_CYCLE_TIME!=0) {
                Drug_Deliverer(DRUG_DIFF_RATE,MAX_TOLERATED_DOSE,false);
                DRUG_CYCLE_ITERATOR++;
            }

            else if(DRUG_CYCLE_ITERATOR%DRUG_CYCLE_TIME==0) {
                Drug_Deliverer(DRUG_DIFF_RATE,MAX_TOLERATED_DOSE,true);
                DRUG_CYCLE_ITERATOR++;
            }
        }

        ShuffleAgents(rn);
        for (SingleDrugDoseAdjustmentAddVarietyCell cell : this) {
            cell.CellStep();
        }


    }

    public void ModelStepAdaptiveTherapy(int tick) {

        AdaptiveTherapyChecker(tick);

        if (HAS_AT_STARTED) {

            if(DRUG_CYCLE_ITERATOR>=CHECK_TUMORSIZE_INTERVAL_AT) {
                Drug_Deliverer(DRUG_DIFF_RATE,CURRENT_DRUG,false);
                DRUG_CYCLE_ITERATOR=0;
            }

            else if (DRUG_CYCLE_ITERATOR%DRUG_CYCLE_TIME!=0) {
                Drug_Deliverer(DRUG_DIFF_RATE,CURRENT_DRUG,false);
                DRUG_CYCLE_ITERATOR++;
            }

            else if (DRUG_CYCLE_ITERATOR%DRUG_CYCLE_TIME==0) {
                Drug_Deliverer(DRUG_DIFF_RATE,CURRENT_DRUG,true);
                DRUG_CYCLE_ITERATOR++;
            }
        }

        ShuffleAgents(rn);
        for (SingleDrugDoseAdjustmentAddVarietyCell cell : this) {
            cell.CellStep();
        }
    }

    public void Drug_Deliverer(double drug_diffusion_rate, double drug_amount, boolean is_drug_on) {

        drug.MulAll(0.9);
//        drug.MulAll(0.956); //first order drug decay kinetics for Tamoxifen per hour calculated after finding elimination rate constant lambda. Lambda is Clearance (CL) over Volume of Distribution (Vd).Vd is 55L/kg and CL is 2.5L/hour. Then C=C0exp(-lambda*t) where t is 1 hour

        if(!IS_DRUG_FROM_PERIPHERY) {
            if (is_drug_on) {
                drug.AddAll(drug_amount);
            }
            drug.DiffusionADI(drug_diffusion_rate);
        }

        else if(IS_DRUG_FROM_PERIPHERY) {
            if (is_drug_on) {
                drug.DiffusionADI(drug_diffusion_rate, drug_amount);
            }
        }

//        if(is_drug_on) {
//            drug.AddAll(drug_amount);
//        }
//        drug.MulAll(0.956); //first order drug decay kinetics for Tamoxifen per hour calculated after finding elimination rate constant lambda. Lambda is Clearance (CL) over Volume of Distribution (Vd).Vd is 55L/kg and CL is 2.5L/hour. Then C=C0exp(-lambda*t) where t is 1 hour
//        drug.DiffusionADI(drug_diffusion_rate);

    }

    public void DrawModel(UIGrid vis, int iModel,int tick) {
        tickLabel.SetText("Tick:  "+tick);
        switch (iModel) {
            case 0:
//                popLabel.SetText("Pop No Drug is "+Pop());
//                drugLabel.SetText("Current Drug (No drug case) is "+String.format("%.3f",CURRENT_DRUG) +"       Grid Avg is "+String.format("%.3f",drug.GetAvg()));
                break;
            case 1:
//                popLabel.SetText("Pop Continuous Therapy is "+Pop());
//                drugLabel.SetText("Current Drug CT is "+String.format("%.3f",CURRENT_DRUG)+"       Grid Avg is "+String.format("%.3f",drug.GetAvg()));
                break;
            case 2:
//                popLabel.SetText("Pop Metronomic is " +Pop());
//                drugLabel.SetText("Current Drug Metronomic is "+String.format("%.3f",CURRENT_DRUG)+"       Grid Avg is "+String.format("%.3f",drug.GetAvg()));
                break;
            case 3:
                popLabel.SetText("Pop Adaptive Therapy is "+Pop());
                drugLabel.SetText("Current Drug Adaptive Therapy is "+String.format("%.3f",CURRENT_DRUG)+"    Grid Avg is "+String.format("%.3f",drug.GetAvg())+"     Drug cycle iterator is "+DRUG_CYCLE_ITERATOR);
                break;
        }
        for (int i = 0; i < length; i++) {
            SingleDrugDoseAdjustmentAddVarietyCell drawMe = GetAgent(i);
            //if the cell does not exist, draw the drug concentration
            vis.SetPix(ItoX(i)+iModel*xDim,ItoY(i), drawMe == null ? HeatMapBRG(drug.Get(i)) : drawMe.type);
        }
        vis.TickPause(0);
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
        CURRENT_POP = (int)rn.Gaussian(CURRENT_POP,MEASUREMENT_NOISE_SD);
//        System.out.println("Tumor measurement after noise is " +CURRENT_POP);


        if(!HAS_AT_STARTED) {

            if(CURRENT_POP>=TUMOR_SIZE_TRIGGERING_AT) {
                HAS_AT_STARTED=true;
                CURRENT_DRUG=MAX_TOLERATED_DOSE;
                System.out.println("Tick: "+tick+" Current Drug for first dose and Current Pop "+CURRENT_POP+" >= Tumor size triggering AT is "+CURRENT_DRUG+". Prev pop is "+PREV_POP);
                INITIAL_POP=CURRENT_POP;
                AT_START_TICK=tick;
                PREV_POP=CURRENT_POP;
                PREV_DRUG=CURRENT_DRUG;
                IS_TREATMENT_VACATION_ON=false;
                return true;

            }
        }

        if (HAS_AT_STARTED) {

            if ((tick-AT_START_TICK)%CHECK_TUMORSIZE_INTERVAL_AT==0) {

                FINAL_POP=CURRENT_POP;
                VARIABLE_BETA=INITIAL_POP>FINAL_POP?INITIAL_POP-FINAL_POP:0;
                System.out.println("Variable Beta is "+VARIABLE_BETA);


                if(CURRENT_POP<=(0.5*TUMOR_SIZE_TRIGGERING_AT)) { //Treatment Skipping. Comment it out to exclude it.
                    CURRENT_DRUG=0.0;
                    System.out.println("Tick: "+tick+" Current Drug for CurrentPop "+CURRENT_POP+" <=0.5 of the tumor size triggering AT is "+CURRENT_DRUG+". Prev pop is "+PREV_POP);
                    PREV_POP=CURRENT_POP;
                    if (!IS_TREATMENT_VACATION_ON) {
                        PREV_DRUG=(0.5*PREV_DRUG<=MIN_DRUG_DOSE)?MIN_DRUG_DOSE:0.5*PREV_DRUG;
                        IS_TREATMENT_VACATION_ON=true;
                    }
                    else if (IS_TREATMENT_VACATION_ON) {
                        PREV_DRUG=PREV_DRUG;
                    }
                    return true;
                }
                if(CURRENT_POP>(int)(0.95*length)) {
                    CURRENT_DRUG=MAX_TOLERATED_DOSE;
                    System.out.println("Tick: "+tick+" Current Drug for pop "+CURRENT_POP+" >0.95*carrying capacity(xDim*yDim)  "+CURRENT_DRUG+". Prev pop is "+PREV_POP);
                    PREV_POP=CURRENT_POP;
                    PREV_DRUG=CURRENT_DRUG;
                    IS_TREATMENT_VACATION_ON=false;
                    return true;

                }
                if(CURRENT_POP>((int)(((1+AT_BETA)*PREV_POP)))) {

                    CURRENT_DRUG=((1+AT_ALPHA)*PREV_DRUG)>=MAX_TOLERATED_DOSE?MAX_TOLERATED_DOSE:((1+AT_ALPHA)*PREV_DRUG);
                    System.out.println("Tick: "+tick+" Current Drug for CurrentPop "+CURRENT_POP+" >(1+at_beta)*Prev Pop "+PREV_POP+" is "+CURRENT_DRUG);
                    PREV_POP=CURRENT_POP;
                    PREV_DRUG=CURRENT_DRUG;
                    IS_TREATMENT_VACATION_ON=false;
                    return true;
                }

                if(CURRENT_POP<=((int)(((1-AT_BETA)*PREV_POP)))) {

                    CURRENT_DRUG=((1-AT_ALPHA)*PREV_DRUG)<=MIN_DRUG_DOSE?MIN_DRUG_DOSE:(1-AT_ALPHA)*PREV_DRUG;
                    System.out.println("Tick: "+tick+" Current Drug for CurrentPop "+CURRENT_POP+" <=(1-at_beta)*Prev Pop "+PREV_POP+" is "+CURRENT_DRUG);
                    PREV_POP=CURRENT_POP;
                    PREV_DRUG=CURRENT_DRUG;
                    IS_TREATMENT_VACATION_ON=false;
                    return true;
                }

                if(CURRENT_POP>((int)((1-AT_BETA)*PREV_POP)) && CURRENT_POP<=((int)((1+AT_BETA)*PREV_POP))) {
                    if (VARIABLE_BETA>0 && CURRENT_POP<INITIAL_POP) {
                        CURRENT_DRUG=PREV_DRUG;
                        System.out.println("Tick: "+tick+" Current Drug for Current Pop "+CURRENT_POP+" within the threshold(beta) of prev pop "+PREV_POP+" is "+CURRENT_DRUG);
                        PREV_POP=CURRENT_POP;
                        PREV_DRUG=CURRENT_DRUG;
//                        IS_AT_ON=true;
                        IS_TREATMENT_VACATION_ON=false;
                        return true;
                    } else {
                        CURRENT_DRUG=((1+AT_ALPHA)*PREV_DRUG)>=MAX_TOLERATED_DOSE?MAX_TOLERATED_DOSE:((1+AT_ALPHA)*PREV_DRUG);
                        System.out.println("Tick: "+tick+" Current Drug for CurrentPop "+CURRENT_POP+" >(1+at_beta)*Prev Pop "+PREV_POP+" is "+CURRENT_DRUG);
                        PREV_POP=CURRENT_POP;
                        PREV_DRUG=CURRENT_DRUG;
//                        IS_AT_ON=true;
                        IS_TREATMENT_VACATION_ON=false;
                        INITIAL_POP=CURRENT_POP;
                        return true;
                    }
                }

//                if(CURRENT_POP>((int)((1-AT_BETA)*PREV_POP)) && CURRENT_POP<=((int)((1+AT_BETA)*PREV_POP))) {
//                    CURRENT_DRUG=PREV_DRUG;
//                    System.out.println("Tick: "+tick+" Current Drug for Current Pop "+CURRENT_POP+" within the threshold(beta) of prev pop "+PREV_POP+" is "+CURRENT_DRUG);
//                    PREV_POP=CURRENT_POP;
//                    PREV_DRUG=CURRENT_DRUG;
//                    IS_TREATMENT_VACATION_ON=false;
//                    return true;
//                }

                return false;
            }
        }

        return false;

    }
}

class SingleDrugDoseAdjustmentAddVarietyCell extends AgentSQ2D<SingleDrugDoseAdjustmentAddVarietyGrid> {

//    public double generation_time;
//    public double drug_sensitivity_score;
    public int type;

    public void Mutate() {
        if(type==G.SENSITIVE && G.rn.Double(1)<=G.Mut_SenToRes) {
            type=G.RESISTANT;
            G.SEN_POP--;
            G.RES_POP++;

        }
        else if (type==G.RESISTANT && G.rn.Double(1)<=G.Mut_ResToSen) {
            type=G.SENSITIVE;
            G.RES_POP--;
            G.SEN_POP++;
        } else {
            type=type;
        }
    }

    public void DivideAndMutate(int index) {
        SingleDrugDoseAdjustmentAddVarietyCell tmp = G.NewAgentSQ(index);

        tmp.type=this.type;
        if(this.type==G.SENSITIVE) {
            G.SEN_POP++;
        }
        if(this.type==G.RESISTANT) {
            G.RES_POP++;
        }
        this.Mutate();
        tmp.Mutate();
    }


    public void CellStep() {

//        Mutate();

        //Consumption of Drug
        G.drug.Mul(Isq(), G.DRUG_UPTAKE);
        //Chance of Death, depends on resistance and drug concentration
        double tmp_random = G.rn.Double(1);
        if(type==G.SENSITIVE && tmp_random<(G.DEATH_PROB+G.drug.Get(Isq())*G.DRUG_DEATH)) {
            Dispose();
            G.SEN_POP--;
            return;
        }
        if(type==G.RESISTANT && tmp_random<G.DEATH_PROB) {
            Dispose();
            G.RES_POP--;
            return;
        }
        //Chance of Division, depends on resistance
        double tmp_div_prob;
        if (this.type==G.SENSITIVE) {
            tmp_div_prob=G.DIV_PROB_SEN;
        }
        else if (this.type==G.RESISTANT) {
            tmp_div_prob=G.DIV_PROB_RES;
        } else {
            throw new IllegalArgumentException("The cell type does not match to any of the existing types");
        }
        if (G.rn.Double(1) < tmp_div_prob) {

            int options_empty=MapEmptyHood(G.divHood);
            if(options_empty>0){
                int myTmpIndex=G.divHood[G.rn.Int(options_empty)];
                DivideAndMutate(myTmpIndex);
            }

            if (options_empty==0 && G.rn.Double(1)<G.REPLACEMENT_THRESHOLD) { //&& this.type==G.RESISTANT) {

                int options_occupied=MapOccupiedHood(G.divHood);
//                System.out.println("options_occupied is " + options_occupied);
                int tmpAgentIndex=G.divHood[G.rn.Int(options_occupied)];
//                System.out.println("tmpAgentIndex is " + tmpAgentIndex);
                SingleDrugDoseAdjustmentAddVarietyCell tmpAgent = G.GetAgent(tmpAgentIndex);
                int tmpAgentType = tmpAgent.type;

                tmpAgent.Dispose();
                if(tmpAgentType==G.SENSITIVE) {
                    G.SEN_POP--;
                }
                if(tmpAgentType==G.RESISTANT) {
                    G.RES_POP--;
                }
                DivideAndMutate(tmpAgentIndex);



            }

        }
//        Mutate();

    }

}
