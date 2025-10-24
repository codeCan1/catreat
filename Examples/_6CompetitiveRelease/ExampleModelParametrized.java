package Examples._6CompetitiveRelease;

import Framework.GridsAndAgents.AgentGrid2D;
import Framework.GridsAndAgents.AgentSQ2Dunstackable;
import Framework.GridsAndAgents.PDEGrid2D;
import Framework.Gui.GridWindow;
import Framework.Gui.UIGrid;
import Framework.Rand;
import Framework.Tools.FileIO;
import static Framework.Util.*;

public class ExampleModelParametrized extends AgentGrid2D<ExampleCellParametrized> {
    //model constants
    public final static int RESISTANT = RGB(0, 1, 0), SENSITIVE = RGB(0, 0, 1);
//    public double DIV_PROB_SEN = 0.025, DIV_PROB_RES = 0.01, DEATH_PROB = 0.001 ; (0.02,0.01,0.01 works well)

    public double DIV_PROB_SEN, DIV_PROB_RES, DEATH_PROB;
    public double DRUG_START = 400, DRUG_PERIOD = 200, DRUG_DURATION = 40, DRUG_DIFF_RATE = 2, DRUG_UPTAKE = 0.91, DRUG_DEATH = 0.2, DRUG_BOUNDARY_VAL = 1.0;
    //internal model objects
    public PDEGrid2D drug;
    public Rand rn;
    public int[] divHood = MooreHood(false);

    public ExampleModelParametrized(int xDim, int yDim, Rand rn, double birthSensitiveProb, double birthResistantProb, double deathProb) {
        super(xDim, yDim, ExampleCellParametrized.class);
        this.rn = rn;
        this.DIV_PROB_SEN = birthSensitiveProb;
        this.DIV_PROB_RES = birthResistantProb;
        this.DEATH_PROB = deathProb;
        drug = new PDEGrid2D(xDim, yDim);
    }

    public static void main(String[] args) {
        int x = 100, y = 100, visScale = 5, tumorRad = 10, msPause = 0;
        double resistantProp = 0.5;
        GridWindow win = new GridWindow("Competitive Release", x*3, y, visScale,true);
        ExampleModelParametrized[] models = new ExampleModelParametrized[3];
        FileIO popsOut=new FileIO("populations.csv","w");

        double birthProbSensitive = Double.parseDouble(args[0]);
        double birthProbResistant = Double.parseDouble(args[1]);
        double deathProb = Double.parseDouble(args[2]);
        System.out.println(birthProbResistant);


        for (int i = 0; i < models.length; i++) {
            models[i]=new ExampleModelParametrized(x,y,new Rand(1),birthProbSensitive,birthProbResistant,deathProb);
            models[i].InitTumor(tumorRad, resistantProp);
        }
        models[0].DRUG_DURATION =0;//no drug
        models[1].DRUG_DURATION =models[1].DRUG_PERIOD;//constant drug
        //Main run loop
        for (int tick = 0; tick < 10000; tick++) {
            win.TickPause(msPause);
            for (int i = 0; i < models.length; i++) {
                models[i].ModelStep(tick);
                models[i].DrawModel(win,i);
            }
            //data recording
            popsOut.Write(models[0].Pop()+","+models[1].Pop()+","+models[2].Pop()+"\n");
            if((tick)%100==0) {
                //        win.ToPNG("ModelsTick" +tick+".png");
            }
        }
        popsOut.Close();
        win.Close();
    }

    public void InitTumor(int radius, double resistantProb) {
        //get a list of indices that fill a circle at the center of the grid
        int[] tumorNeighborhood = CircleHood(true, radius);
        int hoodSize=MapHood(tumorNeighborhood,xDim/2,yDim/2);
        for (int i = 0; i < hoodSize; i++) {
            NewAgentSQ(tumorNeighborhood[i]).type = rn.Double() < resistantProb ? RESISTANT : SENSITIVE;
        }
    }

    public void ModelStep(int tick) {
        ShuffleAgents(rn);
        for (ExampleCellParametrized cell : this) {
            cell.CellStep();
        }
        //check if drug should enter through the boundaries
        if (tick > DRUG_START && (tick - DRUG_START) % DRUG_PERIOD < DRUG_DURATION) {
            drug.DiffusionADI(DRUG_DIFF_RATE, DRUG_BOUNDARY_VAL);
        } else {
            drug.DiffusionADI(DRUG_DIFF_RATE);
        }
    }

    public void DrawModel(UIGrid vis, int iModel) {
        for (int i = 0; i < length; i++) {
            ExampleCellParametrized drawMe = GetAgent(i);
            //if the cell does not exist, draw the drug concentration
            vis.SetPix(ItoX(i)+iModel*xDim,ItoY(i), drawMe == null ? HeatMapRGB(drug.Get(i)) : drawMe.type);
        }
    }
}
class ExampleCellParametrized extends AgentSQ2Dunstackable<ExampleModelParametrized> {
    public int type;

    public void CellStep() {
        //Consumption of Drug
        G.drug.Mul(Isq(), G.DRUG_UPTAKE);
        //Chance of Death, depends on resistance and drug concentration
        if (G.rn.Double() < G.DEATH_PROB + (type == G.RESISTANT ? 0 : G.drug.Get(Isq()) * G.DRUG_DEATH)) {
            Dispose();
            return;
        }
        //Chance of Division, depends on resistance
        else if (G.rn.Double() < (type == G.RESISTANT ? G.DIV_PROB_RES : G.DIV_PROB_SEN)) {
            int options=MapEmptyHood(G.divHood);
            if(options>0){
                G.NewAgentSQ(G.divHood[G.rn.Int(options)]).type=this.type;
            }
        }
    }
}
