
import java.awt.event.*;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 * Snake canvas object for adding to JFrames
 * @author Patrick O'Connor, Peter Tamraz
 * @version 16/01/2014
 */

public class Snake4D extends Canvas implements Runnable{
  
  private static final int WIDTH = 1440;
  private static final int HEIGHT = 900;
  private static final int SNAKE_MAXL = 65536;
  private static final int BLOCK_DIM = 16;
  private static final int GRID_PADX = 72;
  private static final int GRID_PADY = 16;
  private static final int GRID_DIMPX = 384;
  private static final int GRID_DIM = 24;
  private static final int DIR_UP = 0;
  private static final int DIR_DOWN = 1;
  private static final int DIR_LEFT = 2;
  private static final int DIR_RIGHT = 3;
  private static final int DIR_IN = 4;
  private static final int DIR_OUT = 5;
  private static final int DIR_WIN = 6;
  private static final int DIR_WOUT = 7;
  
  private int eggx;
  private int eggy;
  private int eggw;
  private int eggz;
  private int snakeLength;
  private int highScore;
  private int[] sX = new int[SNAKE_MAXL]; 
  private int[] sY = new int[SNAKE_MAXL];
  private int[] sZ = new int[SNAKE_MAXL];
  private int[] sW = new int[SNAKE_MAXL]; 
  private int direction;
  
  private boolean gameRunning = false;
  private boolean eggPlaced = false;
  
  private static boolean running = false;
  private Thread gameThread;
  private BufferedImage snakeblock;
  private BufferedImage egg;
  private BufferedImage board;
  
  public Snake4D(int themeNum){
    try{
      egg = ImageIO.read(new File("egg.png"));
      snakeblock = ImageIO.read(new File("snake.png"));
      switch(themeNum){
        case 1: board = ImageIO.read(new File("blackgreen.png"));
        break;
        case 2: board = ImageIO.read(new File("blackmagenta.png"));
        break;
        case 3: board = ImageIO.read(new File("blackred.png"));
        break;
        case 4: board = ImageIO.read(new File("blueblack.png"));
        break;
        case 5: board = ImageIO.read(new File("christmas.png"));
        break;
        case 6: board = ImageIO.read(new File("futurelook.png"));
        break;
        case 7: board = ImageIO.read(new File("halloween.png"));
        break;
        case 8: board = ImageIO.read(new File("metal.png"));
        break;
        case 9: board = ImageIO.read(new File("rust.png"));
        break;
        case 10: board = ImageIO.read(new File("sky.png"));
        break; 
      }
    } catch(IOException e){
    }
    addKeyListener(new KeyListener());
    setFocusable(true);
  }//Constructor
  

  
  /**
   * Starts the game thread.
   */
  public synchronized void start(){ 
    if(running)
      return;
    running = true;
    gameThread = new Thread(this);
    gameThread.start(); 
  }//start
  
  /**
   * Stops the game thread.
   */
  public synchronized void stop(){ 
    if(!running)
      return;
    running = true;
    try{
      gameThread.join();
    }catch(InterruptedException e){
      e.printStackTrace();
    }
  }//stop
  
  /**
   * This method contains the statements that run in the game thread. Includes
   * the main game loop.
   */
  public void run(){
    
    long lastTime = System.nanoTime(); //This stuff limits the  game loop to iterating amountOfTicks per second
    final double amountOfTicks = 15.0; 
    double ns = 1000000000 / amountOfTicks;
    double delta = 0; 
    
    while(running){
      long now = System.nanoTime();
      delta += (now - lastTime) / ns;
      lastTime = now;
      if(delta >= 1){
        tick();
        delta--;
      }
      render();
    }
    stop();
  }//run
  
  /**
   * Contains all of the background calculations that need
   * to be calculated before the images
   * can be drawn on the screen for the next frame.
   */
  public void tick(){
    if(!gameRunning)
      startGame();
    if(gameRunning){
      placeEgg();
      moveSnake();
      checkEggCollision();
      checkWallSelfCollision();
    }
  }//tick
  
  /**
   * This method draws the images of the game to their
   * correct locations determined by tick() 
   */
  public void render(){
    BufferStrategy bs = this.getBufferStrategy(); //
    if(bs == null){                               
      createBufferStrategy(3);                    
      return;
    }
    Graphics g = bs.getDrawGraphics();
    Font biggerText = g.getFont().deriveFont((float)16);
    g.setFont(biggerText);
    g.setColor(Color.ORANGE);
    g.fillRect(0, 0, WIDTH, HEIGHT);
    g.drawImage(board,0, 0, null);
    
    
    //Draws snake and egg on top-left (XY) quadrant
    g.drawImage(egg,(eggx * BLOCK_DIM) + GRID_PADX,(eggy * BLOCK_DIM) + GRID_PADY,null);
    for(int i = 0; i < snakeLength; i++)
      g.drawImage(snakeblock,(sX[i] * BLOCK_DIM) + GRID_PADX,sY[i] * BLOCK_DIM + GRID_PADY ,null);
    
    //Draws snake and egg on top-center (XZ) quadrant
    g.drawImage(egg,(eggx * BLOCK_DIM) + GRID_PADX * 2 + GRID_DIMPX,(eggz * BLOCK_DIM) + GRID_PADY,null);
    for(int i = 0; i < snakeLength; i++)
      g.drawImage(snakeblock,(sX[i] * BLOCK_DIM) + GRID_PADX * 2 + GRID_DIMPX,(sZ[i] * BLOCK_DIM) + GRID_PADY,null);
    
    //Draws snake and egg on top-right (XW)quadrant
    g.drawImage(egg,(eggx * BLOCK_DIM) + GRID_PADX * 3 + GRID_DIMPX * 2,(eggw * BLOCK_DIM) + GRID_PADY,null);
    for(int i = 0; i < snakeLength; i++)
      g.drawImage(snakeblock,(sX[i] * BLOCK_DIM) + GRID_PADX * 3 + GRID_DIMPX * 2,(sW[i] * BLOCK_DIM) + GRID_PADY,null);
    
    //Draws snake and egg on bottom-left (ZY)quadrant
    g.drawImage(egg,(eggz * BLOCK_DIM) + GRID_PADX,(eggy * BLOCK_DIM) + GRID_PADY * 3 + GRID_DIMPX,null);
    for(int i = 0; i < snakeLength; i++)
      g.drawImage(snakeblock,(sZ[i] * BLOCK_DIM) + GRID_PADX,(sY[i] * BLOCK_DIM) + GRID_PADY * 3 + GRID_DIMPX,null);
    
    //Draws snake and egg on bottom-center (WY)quadrant
    g.drawImage(egg,(eggw * BLOCK_DIM) + GRID_PADX * 2 + GRID_DIMPX,
                (eggy * BLOCK_DIM) + GRID_PADY * 3 + GRID_DIMPX,null);
    for(int i = 0; i < snakeLength; i++)
      g.drawImage(snakeblock,(sW[i] * BLOCK_DIM) + GRID_PADX * 2 + GRID_DIMPX,
                  (sY[i] * BLOCK_DIM) + GRID_PADY * 3 + GRID_DIMPX,null);
    
    //Draws snake and egg on bottom-left (WZ)quadrant
    g.drawImage(egg,(eggw * BLOCK_DIM) + GRID_PADX * 3 + GRID_DIMPX * 2,
                (eggz * BLOCK_DIM) + GRID_PADY * 3 + GRID_DIMPX,null);
    for(int i = 0; i < snakeLength; i++)
      g.drawImage(snakeblock,(sW[i] * BLOCK_DIM) + GRID_PADX * 3 + GRID_DIMPX * 2,
                  (sZ[i] * BLOCK_DIM) + GRID_PADY * 3 + GRID_DIMPX,null);
    
    g.drawString("Score: " + snakeLength, 100, 860);
    g.drawString("Egg location: " + eggx + "," + eggy + "," + eggz + "," + eggw, 413, 860);
    g.drawString("High score: " + highScore, 826, 860);
    g.drawString("Head location: " + sX[0] + "," + sY[0] + "," + sZ[0] + "," + sW[0], 1240, 860);
    
    g.dispose();
    bs.show();
  }//render
  
  /**
   * Starts the game by Placing the snake at the center
   * of the boards and restarting its direction.
   */
  public void startGame(){
    gameRunning = true;
    direction = 0;
    snakeLength = 1;
    highScore = readScore();
    sX[0] = 8;
    sY[0] = 8;
    sZ[0] = 8;
    sW[0] = 8;
  }//startGame
  
  /**
   * Generates a random location for the egg while 
   * avoiding spaces occupied by the snake itself.
   */
  public void placeEgg(){
    for(int i = snakeLength; i > 0; i--){ 
      if(!eggPlaced){ 
        eggx = ((int)(Math.random()*GRID_DIM));
        eggy = ((int)(Math.random()*GRID_DIM));
        eggz=  ((int)(Math.random()*GRID_DIM));
        eggw = ((int)(Math.random()*GRID_DIM));
        eggPlaced = true;
      }
      else if(eggx == sX[i] && eggy == sY[i] && eggz == sZ[i] && eggw == sW[i]){
        eggPlaced = false;
        i = snakeLength;
      }
    }
  }//placeEgg
  
  /**
   * Moves the snake block at [0] ahead by one block
   * based on the direction it is moving and sets the 
   * X and Y of the other blocks to the X and Y of the 
   * block ahead of it to move the snake forward.
   */
  public void moveSnake(){
    
    for(int i = snakeLength; i > 0; i--){
      sX[i]=sX[(i - 1)];
      sY[i]=sY[(i - 1)];
      sZ[i]=sZ[(i - 1)];
      sW[i]=sW[(i - 1)];
    }
    
    switch(direction){
      case DIR_UP: sY[0] -= 1;
      break;
      case DIR_DOWN: sY[0] += 1;
      break;
      case DIR_LEFT: sX[0] -= 1;
      break;
      case DIR_RIGHT: sX[0] += 1;
      break;
      case DIR_IN: sZ[0] -= 1;
      break;
      case DIR_OUT: sZ[0] += 1;
      break;
      case DIR_WIN: sW[0] -= 1;
      break;
      case DIR_WOUT: sW[0] += 1;
      break;
    }
  }//moveSnake
  
  /**
   * Checks if the snake block at [0] is at the same
   * location as the egg and increases the length of the snake if 
   * it is.
   */
  public void checkEggCollision(){
    if(sY[0] == eggy && sX[0] == eggx && sZ[0] == eggz && sW[0] == eggw){
      snakeLength++;
      if(highScore < snakeLength){
        highScore = snakeLength;
        writeScore(highScore);
      }
      eggPlaced = false;
    }
  }//checkEggCollision
  
  /** 
   * Checks if the snake block at [0] is colliding 
   * with another snake block or the borders of the board.
   */
  public void checkWallSelfCollision(){
    if(sY[0] < 0 || sY[0] > 24 || sX[0] < 0 || sX[0] >= 24)
      gameRunning = false;
    
    if(sZ[0] < 0 || sZ[0] > 24 || sW[0] < 0 || sW[0] >= 24)
      gameRunning = false;
    
    for(int i = snakeLength; i > 0; i-- ){
      if(sY[0] == sY[i] && sX[0] == sX[i] && sZ[0] == sZ[i] && sW[0] == sW[i] )
        gameRunning = false;
    }
  }//checkWallSelfCollision
  
  /**
   * Creates an event listener that listens for
   * keyboard events
   */
  private class KeyListener extends KeyAdapter{
    /**
     * changes the direction of the snake based
     * on keyboard inputs.
     * @param KeyEvent e
     */
    public void keyPressed(KeyEvent e){
      int key = e.getKeyCode();
      if(key == KeyEvent.VK_UP){
        if(direction != DIR_DOWN){
          direction = DIR_UP;
        }
      }
      else if(key == KeyEvent.VK_DOWN){
        if(direction != DIR_UP){
          direction = DIR_DOWN;
        }
      }
      else if(key == KeyEvent.VK_LEFT){
        if(direction != DIR_RIGHT){
          direction = DIR_LEFT;
        }
      }
      else if(key == KeyEvent.VK_RIGHT){
        if(direction != DIR_LEFT){
          direction = DIR_RIGHT;
        }
      }
      else if(key == KeyEvent.VK_S){
        if(direction != DIR_IN){
          direction = DIR_OUT;   
        }
      }
      else if(key == KeyEvent.VK_W){
        if(direction != DIR_OUT){
          direction = DIR_IN;
        }
      }
      else if(key == KeyEvent.VK_A){
        if(direction != DIR_WOUT){
          direction = DIR_WIN;
        }
      }
      else if(key == KeyEvent.VK_D){
        if(direction != DIR_WIN){
          direction = DIR_WOUT;
        }
      }
      
    }//keyPressed
  }//KeyListener
  
  /**
   * Saves the highscore to a text file
   * @param snakeLength
   */
  private static void writeScore(int snakeLength) {
    
    String snakeLengthStr;
    String fileName = "scores.txt";
    
    try{
      FileWriter fileWriter =
        new FileWriter(fileName);
      BufferedWriter bufferedWriter =
        new BufferedWriter(fileWriter);
      
      snakeLengthStr = Integer.toString(snakeLength);
      bufferedWriter.write(snakeLengthStr);
      
      bufferedWriter.close();
    }
    catch(IOException ex){
      System.out.println("Error writing to file '" + fileName + "'");
    }
  }//writeScore
  
  /**
   * Reads the highscore from a text file
   * @return int
   */
  private static int readScore(){
    
    String fileName = "scores.txt";
    String line = null;
    int highScore = 0; 
    
    try {
      FileReader fileReader = 
        new FileReader(fileName);
      BufferedReader bufferedReader = 
        new BufferedReader(fileReader);
      
      while((line = bufferedReader.readLine()) != null){
        highScore = Integer.parseInt(line);
      }    
      bufferedReader.close();            
    }
    catch(FileNotFoundException ex){
      System.out.println("Unable to open file '" + fileName + "'");                
    }
    catch(IOException ex){
      System.out.println("Error reading file '" + fileName + "'"); 
    }
    return highScore;
  }//readScore
  
  
  /**
   * Main menu for running snake4D
   * @author Peter Tamraz
   * @version 19/01/15
   */
  private static class MainMenu extends JApplet {
    JLabel jlab;
    JComboBox<String> jcb;
    int themeNum = 1;
    String themeOptions[] = { "Black and green", "Black and magenta", "Black and red", "Black and blue", "Christmas",
      "Future Style", "Halloween", "Metal", "Rust", "Sky" };
    
    MainMenu(){
      final JFrame jfrm = new JFrame("Snakes on a Plane");
      jfrm.setLayout(null);
      jfrm.setSize(500, 500);
      jfrm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      
      Container conJfrm = jfrm.getContentPane();
      conJfrm.setBackground(Color.GRAY);
      
      JButton jbtnStart = new JButton("Let me play already");
      JButton jbtnHelp = new JButton("Help!");
      jcb = new JComboBox<String>(themeOptions);
      
      jcb.addActionListener(new ActionListener(){
        public void actionPerformed(ActionEvent ae){
          if (themeOptions[0].equals(jcb.getSelectedItem())){
            themeNum = 1;
          }  else if (themeOptions[1].equals(jcb.getSelectedItem())){
            themeNum = 2;
            
          }  else if (themeOptions[2].equals(jcb.getSelectedItem())){
            themeNum = 3;
            
          }  else if (themeOptions[3].equals(jcb.getSelectedItem())){
            themeNum = 4;
            
          }  else if (themeOptions[4].equals(jcb.getSelectedItem())){
            themeNum = 5;
            
          }  else if (themeOptions[5].equals(jcb.getSelectedItem())){
            themeNum = 6;
            
          }  else if (themeOptions[6].equals(jcb.getSelectedItem())){
            themeNum = 7;
            
          }  else if (themeOptions[7].equals(jcb.getSelectedItem())){
            themeNum = 8;
            
          }  else if (themeOptions[8].equals(jcb.getSelectedItem())){
            themeNum = 9;
            
          }  else if (themeOptions[9].equals(jcb.getSelectedItem())){
            themeNum = 10;
            
          } else {
            themeNum = 1;
          }
        }//actionPerformed
      });
      
      jbtnStart.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          Snake4D game = new Snake4D(themeNum);
          JFrame snakeFrame = new JFrame("Snake");
          snakeFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
          snakeFrame.setSize(1440,900);
          snakeFrame.setResizable(false);
          snakeFrame.setLocationRelativeTo(null); 
          snakeFrame.add(game);
          jfrm.setVisible(false);
          snakeFrame.setVisible(true);
          game.start();
        }//actionPerformed
      });
      
      jbtnHelp.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          JOptionPane.showMessageDialog(null, "Up/Down arrow keys control y-axis\nleft/right arrow keys control x-axis\na/d keys control w-axis\nw/s keys control z-axis   ");
        }//actionPerformed
      });
      
      jfrm.add(jbtnStart);
      jfrm.add(jbtnHelp);
      jfrm.add(jcb);
      jbtnStart.setSize(150, 28);
      jbtnStart.setLocation(170, 210);
      jbtnHelp.setSize(150, 28);
      jbtnHelp.setLocation(170, 250);
      jcb.setSize(125, 30);
      jcb.setLocation(0, 0);
      jfrm.setLocationRelativeTo(null);
      jfrm.setResizable(false);
      
      jlab = new JLabel("Welcome to Snake 4D");
      jfrm.add(jlab);
      jfrm.setVisible(true);
      jlab.setSize(600, 20);
      jlab.setLocation(185, 0);
      jfrm.requestFocus();
    }//constructor
  }//MainMenu
  
  public static void main(String[] args){
    MainMenu menu = new MainMenu();
  }//main
}//Snake



