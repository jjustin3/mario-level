package dk.itu.mario.level;

import java.util.Random;
import java.util.ArrayList;
import java.lang.Math;

import dk.itu.mario.MarioInterface.Constraints;
import dk.itu.mario.MarioInterface.GamePlay;
import dk.itu.mario.MarioInterface.LevelInterface;
import dk.itu.mario.engine.sprites.SpriteTemplate;
import dk.itu.mario.engine.sprites.Enemy;


public class MyLevel extends Level{
	//Store information about the level
	public int ENEMIES = 0; //the number of enemies the level contains
	public int BLOCKS_EMPTY = 0; // the number of empty blocks
	public int BLOCKS_COINS = 0; // the number of coin blocks
	public int BLOCKS_POWER = 0; // the number of power blocks
	public int COINS = 0; //These are the coins in boxes that Mario collect

	private static Random levelSeedRandom = new Random();
	public static long lastSeed;

	Random random;

	private int difficulty;
    private int type;
	private int gaps;
	private ArrayList<Integer> blocks;
	private ArrayList<ArrayList<Decoration>> decorations;
	private double jumpWeight, cannonWeight, hillWeight, tubeWeight, straightWeight;

	public MyLevel(int width, int height)
    {
		super(width, height);
		blocks = new ArrayList<Integer>();
		decorations = new ArrayList<ArrayList<Decoration>>();
    }

	public MyLevel(int width, int height, long seed, int difficulty, int type, GamePlay playerMetrics)
    {
        this(width, height);
        creat(seed, difficulty, type, playerMetrics);
    }

    public void creat(long seed, int difficulty, int type, GamePlay playerMetrics)
    {
        this.type = type;
        this.difficulty = difficulty;

        lastSeed = seed;
        random = new Random(seed);

        //create the start location
        int length = 0;
        length += buildStraight(0, width, true);

		//construct weights
		createWeights(playerMetrics);

		blocks.clear();
		decorations.clear();

        //create all of the medium sections
        while (length < width - 64)
        {
            decorations.add(new ArrayList<Decoration>());
			length += buildRandomBlock(length, width - length);
        }

        //set the end piece
        int floor = height - 1 - random.nextInt(4);

        xExit = length + 8;
        yExit = floor;

        // fills the end piece
        for (int x = length; x < width; x++)
        {
            for (int y = 0; y < height; y++)
            {
                if (y >= floor)
                {
                    setBlock(x, y, GROUND);
                }
            }
        }

        if (type == LevelInterface.TYPE_CASTLE || type == LevelInterface.TYPE_UNDERGROUND)
        {
            int ceiling = 0;
            int run = 0;
            for (int x = 0; x < width; x++)
            {
                if (run-- <= 0 && x > 4)
                {
                    ceiling = random.nextInt(4);
                    run = random.nextInt(4) + 4;
                }
                for (int y = 0; y < height; y++)
                {
                    if ((x > 4 && y <= ceiling) || x < 1)
                    {
                        setBlock(x, y, GROUND);
                    }
                }
            }
        }

        fixWalls();

    }

	private int buildRandomBlock(int xo, int maxLength) {
		double rDouble = random.nextDouble();

		if(rDouble <= jumpWeight) {
			blocks.add(0);
			return buildJump(xo, maxLength);
		} else if(rDouble <= cannonWeight) {
			blocks.add(1);
			return buildCannons(xo, maxLength);
		} else if(rDouble <= hillWeight) {
			blocks.add(2);
			return buildHillStraight(xo, maxLength);
		} else if(rDouble <= tubeWeight) {
			blocks.add(3);
			return buildTubes(xo, maxLength);
		} else if(rDouble <= straightWeight) {
			blocks.add(4);
			return buildStraight(xo, maxLength, false);
		}
		return 0;
	}

	/*
	 * REVISE EVERYTHING IN HERE!
	 */
	private void createWeights(GamePlay playerMetrics) {
		//weight = baseChance + (chanceIncrement * playerOffset)

		jumpWeight = 5 + 1 * (playerMetrics.jumpsNumber - 22);
		cannonWeight = 1 + 0.5 * playerMetrics.CannonBallKilled - 0.5 * playerMetrics.timesOfDeathByCannonBall;
		hillWeight = 5 + 0.25 * (playerMetrics.aimlessJumps - 10);
		tubeWeight = 3 + 1 * playerMetrics.ChompFlowersKilled - 0.5 * playerMetrics.timesOfDeathByChompFlower;
		straightWeight = 10 + 0.05 * playerMetrics.timeRunningRight + 1 * playerMetrics.kickedShells;

		//normalize -> take average
		double sum = jumpWeight + hillWeight + tubeWeight + straightWeight;
		jumpWeight /= sum;
		cannonWeight /= sum;
		hillWeight /= sum;
		tubeWeight /= sum;
		straightWeight /= sum;

		//offset to cover [0,1] -> straightWeight should be 1
		cannonWeight += jumpWeight;
		hillWeight += cannonWeight;
		tubeWeight += hillWeight;
		straightWeight += tubeWeight;
	}

    private int buildJump(int xo, int maxLength)
    {
		gaps++;
    	//jl: jump length
    	//js: the number of blocks that are available at either side for free
        int js = random.nextInt(4) + 2;
        int jl = random.nextInt(2) + 2;
        int length = js * 2 + jl;

        boolean hasStairs = random.nextInt(3) == 0;

        int floor = height - 1 - random.nextInt(4);
		//run from the start x position, for the whole length
        for (int x = xo; x < xo + length; x++)
        {
            if (x < xo + js || x > xo + length - js - 1)
            {
            	//run for all y's since we need to paint blocks upward
                for (int y = 0; y < height; y++)
                {	//paint ground up until the floor
                    if (y >= floor)
                    {
                        setBlock(x, y, GROUND);
                    }
                  //if it is above ground, start making stairs of rocks
                    else if (hasStairs)
                    {	//LEFT SIDE
                        if (x < xo + js)
                        { //we need to max it out and level because it wont
                          //paint ground correctly unless two bricks are side by side
                            if (y >= floor - (x - xo) + 1)
                            {
                                setBlock(x, y, ROCK);
                            }
                        }
                        else
                        { //RIGHT SIDE
                            if (y >= floor - ((xo + length) - x) + 2)
                            {
                                setBlock(x, y, ROCK);
                            }
                        }
                    }
                }
            }
        }

        return length;
    }

    private int buildCannons(int xo, int maxLength)
    {
        int length = random.nextInt(10) + 2;
        if (length > maxLength) length = maxLength;

        int floor = height - 1 - random.nextInt(4);
        int xCannon = xo + 1 + random.nextInt(4);
        for (int x = xo; x < xo + length; x++)
        {
            if (x > xCannon)
            {
                xCannon += 2 + random.nextInt(4);
            }
            if (xCannon == xo + length - 1) xCannon += 10;
            int cannonHeight = floor - random.nextInt(4) - 1;

            for (int y = 0; y < height; y++)
            {
                if (y >= floor)
                {
                    setBlock(x, y, GROUND);
                }
                else
                {
                    if (x == xCannon && y >= cannonHeight)
                    {
                        if (y == cannonHeight)
                        {
                            setBlock(x, y, (byte) (14 + 0 * 16));
                        }
                        else if (y == cannonHeight + 1)
                        {
                            setBlock(x, y, (byte) (14 + 1 * 16));
                        }
                        else
                        {
                            setBlock(x, y, (byte) (14 + 2 * 16));
                        }
                    }
                }
            }
        }

        return length;
    }

    private int buildHillStraight(int xo, int maxLength)
    {
        int length = random.nextInt(10) + 10;
        if (length > maxLength) length = maxLength;

        int floor = height - 1 - random.nextInt(4);
        for (int x = xo; x < xo + length; x++)
        {
            for (int y = 0; y < height; y++)
            {
                if (y >= floor)
                {
                    setBlock(x, y, GROUND);
                }
            }
        }

        addEnemyLine(xo + 1, xo + length - 1, floor - 1);

        int h = floor;

        boolean keepGoing = true;

        boolean[] occupied = new boolean[length];
        while (keepGoing)
        {
            h = h - 2 - random.nextInt(3);

            if (h <= 0)
            {
                keepGoing = false;
            }
            else
            {
                int l = random.nextInt(5) + 3;
                int xxo = random.nextInt(length - l - 2) + xo + 1;

                if (occupied[xxo - xo] || occupied[xxo - xo + l] || occupied[xxo - xo - 1] || occupied[xxo - xo + l + 1])
                {
                    keepGoing = false;
                }
                else
                {
                    occupied[xxo - xo] = true;
                    occupied[xxo - xo + l] = true;
                    addEnemyLine(xxo, xxo + l, h - 1);
                    if (random.nextInt(4) == 0)
                    {
                        decorate(xxo - 1, xxo + l + 1, h);
                        keepGoing = false;
                    }
                    for (int x = xxo; x < xxo + l; x++)
                    {
                        for (int y = h; y < floor; y++)
                        {
                            int xx = 5;
                            if (x == xxo) xx = 4;
                            if (x == xxo + l - 1) xx = 6;
                            int yy = 9;
                            if (y == h) yy = 8;

                            if (getBlock(x, y) == 0)
                            {
                                setBlock(x, y, (byte) (xx + yy * 16));
                            }
                            else
                            {
                                if (getBlock(x, y) == HILL_TOP_LEFT) setBlock(x, y, HILL_TOP_LEFT_IN);
                                if (getBlock(x, y) == HILL_TOP_RIGHT) setBlock(x, y, HILL_TOP_RIGHT_IN);
                            }
                        }
                    }
                }
            }
        }

        return length;
    }

    private void addEnemyLine(int x0, int x1, int y)
    {
        for (int x = x0; x < x1; x++)
        {
            if (random.nextInt(35) < difficulty + 1)
            {
                int type = random.nextInt(4);

                if (difficulty < 1)
                {
                    type = Enemy.ENEMY_GOOMBA;
                }
                else if (difficulty < 3)
                {
                    type = random.nextInt(3);
                }

                setSpriteTemplate(x, y, new SpriteTemplate(type, random.nextInt(35) < difficulty));
                ENEMIES++;
            }
        }
    }

    private int buildTubes(int xo, int maxLength)
    {
        int length = random.nextInt(10) + 5;
        if (length > maxLength) length = maxLength;

        int floor = height - 1 - random.nextInt(4);
        int xTube = xo + 1 + random.nextInt(4);
        int tubeHeight = floor - random.nextInt(2) - 2;
        for (int x = xo; x < xo + length; x++)
        {
            if (x > xTube + 1)
            {
                xTube += 3 + random.nextInt(4);
                tubeHeight = floor - random.nextInt(2) - 2;
            }
            if (xTube >= xo + length - 2) xTube += 10;

            if (x == xTube && random.nextInt(11) < difficulty + 1)
            {
                setSpriteTemplate(x, tubeHeight, new SpriteTemplate(Enemy.ENEMY_FLOWER, false));
                ENEMIES++;
            }

            for (int y = 0; y < height; y++)
            {
                if (y >= floor)
                {
                    setBlock(x, y,GROUND);

                }
                else
                {
                    if ((x == xTube || x == xTube + 1) && y >= tubeHeight)
                    {
                        int xPic = 10 + x - xTube;

                        if (y == tubeHeight)
                        {
                        	//tube top
                            setBlock(x, y, (byte) (xPic + 0 * 16));
                        }
                        else
                        {
                        	//tube side
                            setBlock(x, y, (byte) (xPic + 1 * 16));
                        }
                    }
                }
            }
        }

        return length;
    }

    private int buildStraight(int xo, int maxLength, boolean safe)
    {
        int length = random.nextInt(10) + 2;

        if (safe)
        	length = 10 + random.nextInt(5);

        if (length > maxLength)
        	length = maxLength;

        int floor = height - 1 - random.nextInt(4);

        //runs from the specified x position to the length of the segment
        for (int x = xo; x < xo + length; x++)
        {
            for (int y = 0; y < height; y++)
            {
                if (y >= floor)
                {
                    setBlock(x, y, GROUND);
                }
            }
        }

        if (!safe)
        {
            if (length > 5)
            {
                decorate(xo, xo + length, floor);
            }
        }

        return length;
    }

    private void decorate(int xStart, int xLength, int floor)
    {
    	//if its at the very top, just return
        if (floor < 1)
        	return;

        //boolean coins = random.nextInt(3) == 0;
        boolean rocks = true;

        //add an enemy line above the box
        addEnemyLine(xStart + 1, xLength - 1, floor - 1);

        int s = random.nextInt(4);
        int e = random.nextInt(4);

        if (floor - 2 > 0) {
            if ((xLength - 1 - e) - (xStart + 1 + s) > 1){
                for(int x = xStart + 1 + s; x < xLength - 1 - e; x++) {
                    setBlock(x, floor - 2, COIN);
                    COINS++;
					recordCoin(x, floor - 2, false); //ADDED IN FOR HISTORICAL PURPOSES
                }
            }
        }

        s = random.nextInt(4);
        e = random.nextInt(4);

        //this fills the set of blocks and the hidden objects inside them
        if (floor - 4 > 0)
        {
            if ((xLength - 1 - e) - (xStart + 1 + s) > 2)
            {
                for (int x = xStart + 1 + s; x < xLength - 1 - e; x++)
                {
                    if (rocks)
                    {
                        if (x != xStart + 1 && x != xLength - 2 && random.nextInt(3) == 0)
                        {
                            if (random.nextInt(4) == 0)
                            {
                                setBlock(x, floor - 4, BLOCK_POWERUP);
                                BLOCKS_POWER++;
								recordPowerup(x, floor - 4); //ADDED IN FOR HISTORICAL PURPOSES
                            }
                            else
                            {	//the fills a block with a hidden coin --> USE PERCOIN
                                setBlock(x, floor - 4, BLOCK_COIN);
                                BLOCKS_COINS++;
								recordCoin(x, floor - 2, false); //ADDED IN FOR HISTORICAL PURPOSES
                            }
                        }
                        else if (random.nextInt(4) == 0)
                        {
                            if (random.nextInt(4) == 0)
                            {
                                setBlock(x, floor - 4, (byte) (2 + 1 * 16));
								BLOCKS_POWER++; //ADDED IN
								recordPowerup(x, floor - 4); //ADDED IN FOR HISTORICAL PURPOSES
                            }
                            else
                            {
                                setBlock(x, floor - 4, (byte) (1 + 1 * 16));
								BLOCKS_COINS++;
								recordCoin(x, floor - 2, false); //ADDED IN FOR HISTORICAL PURPOSES
                            }
                        }
                        else
                        {
                            setBlock(x, floor - 4, BLOCK_EMPTY);
                            BLOCKS_EMPTY++;
							recordEmptyBlock(x, floor - 4);
                        }
                    }
                }
            }
        }
    }

    private void fixWalls()
    {
        boolean[][] blockMap = new boolean[width + 1][height + 1];

        for (int x = 0; x < width + 1; x++)
        {
            for (int y = 0; y < height + 1; y++)
            {
                int blocks = 0;
                for (int xx = x - 1; xx < x + 1; xx++)
                {
                    for (int yy = y - 1; yy < y + 1; yy++)
                    {
                        if (getBlockCapped(xx, yy) == GROUND){
                        	blocks++;
                        }
                    }
                }
                blockMap[x][y] = blocks == 4;
            }
        }
        blockify(this, blockMap, width + 1, height + 1);
    }

    private void blockify(Level level, boolean[][] blocks, int width, int height){
        int to = 0;
        if (type == LevelInterface.TYPE_CASTLE)
        {
            to = 4 * 2;
        }
        else if (type == LevelInterface.TYPE_UNDERGROUND)
        {
            to = 4 * 3;
        }

        boolean[][] b = new boolean[2][2];

        for (int x = 0; x < width; x++)
        {
            for (int y = 0; y < height; y++)
            {
                for (int xx = x; xx <= x + 1; xx++)
                {
                    for (int yy = y; yy <= y + 1; yy++)
                    {
                        int _xx = xx;
                        int _yy = yy;
                        if (_xx < 0) _xx = 0;
                        if (_yy < 0) _yy = 0;
                        if (_xx > width - 1) _xx = width - 1;
                        if (_yy > height - 1) _yy = height - 1;
                        b[xx - x][yy - y] = blocks[_xx][_yy];
                    }
                }

                if (b[0][0] == b[1][0] && b[0][1] == b[1][1])
                {
                    if (b[0][0] == b[0][1])
                    {
                        if (b[0][0])
                        {
                            level.setBlock(x, y, (byte) (1 + 9 * 16 + to));
                        }
                        else
                        {
                            // KEEP OLD BLOCK!
                        }
                    }
                    else
                    {
                        if (b[0][0])
                        {
                        	//down grass top?
                            level.setBlock(x, y, (byte) (1 + 10 * 16 + to));
                        }
                        else
                        {
                        	//up grass top
                            level.setBlock(x, y, (byte) (1 + 8 * 16 + to));
                        }
                    }
                }
                else if (b[0][0] == b[0][1] && b[1][0] == b[1][1])
                {
                    if (b[0][0])
                    {
                    	//right grass top
                        level.setBlock(x, y, (byte) (2 + 9 * 16 + to));
                    }
                    else
                    {
                    	//left grass top
                        level.setBlock(x, y, (byte) (0 + 9 * 16 + to));
                    }
                }
                else if (b[0][0] == b[1][1] && b[0][1] == b[1][0])
                {
                    level.setBlock(x, y, (byte) (1 + 9 * 16 + to));
                }
                else if (b[0][0] == b[1][0])
                {
                    if (b[0][0])
                    {
                        if (b[0][1])
                        {
                            level.setBlock(x, y, (byte) (3 + 10 * 16 + to));
                        }
                        else
                        {
                            level.setBlock(x, y, (byte) (3 + 11 * 16 + to));
                        }
                    }
                    else
                    {
                        if (b[0][1])
                        {
                        	//right up grass top
                            level.setBlock(x, y, (byte) (2 + 8 * 16 + to));
                        }
                        else
                        {
                        	//left up grass top
                            level.setBlock(x, y, (byte) (0 + 8 * 16 + to));
                        }
                    }
                }
                else if (b[0][1] == b[1][1])
                {
                    if (b[0][1])
                    {
                        if (b[0][0])
                        {
                        	//left pocket grass
                            level.setBlock(x, y, (byte) (3 + 9 * 16 + to));
                        }
                        else
                        {
                        	//right pocket grass
                            level.setBlock(x, y, (byte) (3 + 8 * 16 + to));
                        }
                    }
                    else
                    {
                        if (b[0][0])
                        {
                            level.setBlock(x, y, (byte) (2 + 10 * 16 + to));
                        }
                        else
                        {
                            level.setBlock(x, y, (byte) (0 + 10 * 16 + to));
                        }
                    }
                }
                else
                {
                    level.setBlock(x, y, (byte) (0 + 1 * 16 + to));
                }
            }
        }
    }

	// Same thing as RandomLevel clone but for current level
	public MyLevel clone() throws CloneNotSupportedException {

		MyLevel clone = new MyLevel(width, height);

		clone.xExit = xExit;
    	clone.yExit = yExit;
    	byte[][] map = getMap();
    	SpriteTemplate[][] st = getSpriteTemplate();

    	for (int i = 0; i < map.length; i++)
    		for (int j = 0; j < map[i].length; j++) {
    			clone.setBlock(i, j, map[i][j]);
    			clone.setSpriteTemplate(i, j, st[i][j]);
    	}
    	clone.BLOCKS_COINS = BLOCKS_COINS;
    	clone.BLOCKS_EMPTY = BLOCKS_EMPTY;
    	clone.BLOCKS_POWER = BLOCKS_POWER;
    	clone.ENEMIES = ENEMIES;
    	clone.COINS = COINS;

        return clone;
	}

	private int getDifficulty(GamePlay playerMetrics) {
		//percentage of time allowed to [1-10] difficulty
		int time = 5 * (1 - (playerMetrics.completionTime / playerMetrics.totalTime)); //max of 5

		double deaths =
			playerMetrics.timesOfDeathByArmoredTurtle +
			playerMetrics.timesOfDeathByCannonBall +
			playerMetrics.timesOfDeathByChompFlower +
			playerMetrics.timesOfDeathByFallingIntoGap +
			playerMetrics.timesOfDeathByGoomba +
			playerMetrics.timesOfDeathByGreenTurtle +
			playerMetrics.timesOfDeathByJumpFlower +
			playerMetrics.timesOfDeathByRedTurtle;

		return time;
	}

	public double eval(GamePlay playerMetrics) {
		double blockStdDev = getStdDevOfBlocks();
		double enemyStdDev = getStdDevOfEnemies();
		int enemyDifference = getEnemyDifference(playerMetrics);
		int coinDifference = getCoinDifference(playerMetrics);

		return blockStdDev + enemyStdDev + (enemyDifference / 2) + (coinDifference / 2);
	}

	private double getStdDevOfBlocks() {
		int numJumps = getNumOfBlockType(0);
        int numCannons = getNumOfBlockType(1);
        int numHills = getNumOfBlockType(2);
        int numTubes = getNumOfBlockType(3);
        int numStraights = getNumOfBlockType(4);

        double mean = (numJumps + numCannons + numHills + numTubes + numStraights) / 5;

        double stdDev = Math.pow(mean - numJumps, 2) +
        Math.pow(mean - numCannons, 2) +
        Math.pow(mean - numHills, 2) +
        Math.pow(mean - numTubes, 2) +
        Math.pow(mean - numStraights, 2);
        stdDev /= 4;
        stdDev = Math.sqrt(stdDev);

        return stdDev;
	}

	private double getStdDevOfEnemies() {
        int numGoombas = getNumOfEnemyType(Enemy.ENEMY_GOOMBA);
        int numGreenKoopas = getNumOfEnemyType(Enemy.ENEMY_GREEN_KOOPA);
        int numRedKoopas = getNumOfEnemyType(Enemy.ENEMY_RED_KOOPA);
        int numFlowers = getNumOfEnemyType(Enemy.ENEMY_FLOWER);
        int numSpikys = getNumOfEnemyType(Enemy.ENEMY_SPIKY);

        double mean = (numGoombas + numGreenKoopas + numRedKoopas + numFlowers + numSpikys) / 5;

        double stdDev = Math.pow(mean - numGoombas, 2) +
        Math.pow(mean - numGreenKoopas, 2) +
        Math.pow(mean - numRedKoopas, 2) +
        Math.pow(mean - numFlowers, 2) +
        Math.pow(mean - numSpikys, 2);
        stdDev /= 4;
        stdDev = Math.sqrt(stdDev);

        return stdDev;
    }

    private int getNumOfEnemyType(int type) {
        int count = 0;

        for(ArrayList<Decoration> decs : decorations) {
                for(Decoration dec : decs) {
                        if(dec.type == 0 && dec.enemyType == type)
                                count++;
                }
        }

        return count;
    }

    private int getNumOfBlockType(int type) {
        int count = 0;
        for(Integer i : blocks) {
                if(i == type)
                        count ++;
        }

        return count;
    }

    private int getNumCoinsInBlocks() {
        int count = 0;

        for(ArrayList<Decoration> decs : decorations) {
                for(Decoration dec : decs) {
                        if(dec.type == 2 && dec.blockType == 0)
                                count++;
                }
        }

        return count;
    }

    private int getNumCoinsNotInBlocks() {
        int count = 0;

        for(ArrayList<Decoration> decs : decorations) {
                for(Decoration dec : decs) {
                        if(dec.type == 1)
                                count++;
                }
        }

        return count;
    }


    private int getEnemyDifference(GamePlay player) {
        int maxGoombas = 10;
        int maxTurtles = 12;

        int idealGoombas = (player.GoombasKilled / 8) * maxGoombas;
        int idealTurtles = ((player.RedTurtlesKilled + player.GreenTurtlesKilled) / 8) * maxTurtles;

        //get the difference in the number of enemies there should be
        int numGoombas = getNumOfEnemyType(Enemy.ENEMY_GOOMBA);
        int numGreenKoopas = getNumOfEnemyType(Enemy.ENEMY_GREEN_KOOPA);
        int numRedKoopas = getNumOfEnemyType(Enemy.ENEMY_RED_KOOPA);

        return
        Math.abs(idealGoombas - numGoombas) +
        Math.abs(idealTurtles - (numGreenKoopas + numRedKoopas));
    }

    private int getCoinDifference(GamePlay player) {
        final int maxCoinBlocks = 30;
        final int maxCoins = 75;

        int idealCoins, idealBlocks;
        idealBlocks = (int)(player.percentageCoinBlocksDestroyed * maxCoinBlocks);
        idealCoins = (int)(((float)player.coinsCollected / (float)player.totalCoins) * maxCoins);

        return Math.abs(idealCoins - getNumCoinsNotInBlocks()) +
        Math.abs(idealBlocks - getNumCoinsInBlocks());
    }

    public class Decoration {
        public int x, y;

        /**
         * 0 - enemy
         * 1 - coin
         * 2 - block
         *
         */
        public int type;

        /**
         * 0 - Goomba
         * 1 -
         */
        public int enemyType;


        /**
         * 0 - coin
         * 1 - power up
         * 2 - empty
         */
        public int blockType;
    }

    private void recordCoin(int x, int y, boolean inBlock) {
        Decoration coin = new Decoration();
        coin.x = x;
        coin.y = y;
        if(inBlock) {
                coin.type = 2;
                coin.blockType = 0;
        }
        else {
                coin.type = 1;
        }

        decorations.get(decorations.size()-1).add(coin);
    }

    private void recordPowerup(int x, int y){
        Decoration block = new Decoration();
        block.x = x;
        block.y = y;
        block.type = 2;
        block.blockType = 1;

        decorations.get(decorations.size()-1).add(block);
    }

    private void recordEmptyBlock(int x, int y) {
        Decoration block = new Decoration();
        block.x = x;
        block.y = y;
        block.type = 2;
        block.blockType = 2;

        decorations.get(decorations.size()-1).add(block);
    }

    private void recordEnemy(int x, int y, int type) {
        Decoration enemy = new Decoration();
        enemy.x = x;
        enemy.y = y;
        enemy.type = 0;
        enemy.enemyType = type;

        decorations.get(decorations.size()-1).add(enemy);

    }

}
