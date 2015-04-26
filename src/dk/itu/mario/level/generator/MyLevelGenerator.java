package dk.itu.mario.level.generator;

import java.util.Random;

import dk.itu.mario.MarioInterface.Constraints;
import dk.itu.mario.MarioInterface.GamePlay;
import dk.itu.mario.MarioInterface.LevelGenerator;
import dk.itu.mario.MarioInterface.LevelInterface;
import dk.itu.mario.level.CustomizedLevel;
import dk.itu.mario.level.MyLevel;

public class MyLevelGenerator extends CustomizedLevelGenerator implements LevelGenerator {

	private Random random;

	public LevelInterface generateLevel(GamePlay playerMetrics) {
		LevelInterface level = new MyLevel(320,15,new Random().nextLong(),1,LevelInterface.TYPE_OVERGROUND,playerMetrics);
		level = this.simulatedAnnealing(playerMetrics, level);
		return level;
	}

	@Override
	public LevelInterface generateLevel(String detailedInfo) {
		// TODO Auto-generated method stub
		return null;
	}

	public LevelInterface simulatedAnnealing(GamePlay playerMetrics, LevelInterface interface) throws CloneNotSupportedException {
		MyLevel level = (MyLevel) interface;	// original solution
		MyLevel best = level;	// running best solution
		double temperature = 10;
		double coolingRate = 0.97;
		int iterations = 50;
		double iterationRate = 1.02;
		int totalIterations = 500;
		int numIterations = 0;

		if(random == null)
			random = new Random();

		while(numIterations < totalIterations) {
			for(int i = 0; i < iterations; i++) {
				MyLevel newLevel = level.clone(); //check here
				if((newLevel.eval(playerMetrics) < level.eval(playerMetrics)) || (random.nextDouble() < Math.exp((level.eval(playerMetrics)-newLevel.eval(playerMetrics))/temperature))) {
					level = newLevel;
					if(level.eval(playerMetrics) < best.eval(playerMetrics))
						best = level;
				}
			}

			temperature *= coolingRate;
			iterations *= iterationRate;
		}

		return (LevelInterface) best;
	}

}
