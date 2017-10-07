package org.maeden.controller;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.*;
import java.io.BufferedReader;

/**
 * Simple class for representing 'pre-processed' sensory packets.
 * Agents can bypass the low-level 'raw' sensory data and especially the problem of parsing
 * the contents of the visual field by accessing an 2D array of Lists of world objects.
 *
 * @author: Wayne Iba
 * @version: 2017090901
 */
public class SensoryPacket
{

    static final String NUMLINES = "8";
    JSONArray addingdata;
    String status;
    String smell;
    List<Character> inventory;
    //List<Character>[][] visualArray = (List<Character>[][])new ArrayList[7][5];
    ArrayList<ArrayList<Vector<String>>> visualArray;
    List<Character> groundContents;
    String messages;
    int energy;
    boolean lastActionStatus;
    int worldTime;
    String[] rawSenseData;

    /**
     * constructor that reads the raw data from the server via the provided BufferedReader
     * and performs some amount of preprocessing on that raw data.
     */
    public SensoryPacket(BufferedReader gridIn) {
        visualArray = new ArrayList<ArrayList<Vector<String>>>();
        for (int row = 0; row < 7; row++) {
            visualArray.add(row, new ArrayList<Vector<String>>());
            for (int col = 0; col < 5; col++) {
                visualArray.get(row).add(col, new Vector<String>());
            }
        }
        rawSenseData = getRawSenseDataFromGrid(gridIn);
        initPreProcessedFields(rawSenseData);
    }

    /**
     * another constructor takes in the sensory data as parameters instead of using a BufferedReader
     */
    public SensoryPacket(String inptStatus, String inptSmell, List<Character> inptInventory,
                         ArrayList<ArrayList<Vector<String>>> inptVisualArray,
                         List<Character> inptGroundContents, String inptMessages,
                         Integer inptEnergy, Boolean inptLastActionStatus, Integer inptWorldTime) {
        status = inptStatus;
        smell = inptSmell;
        inventory = inptInventory;
        visualArray = inptVisualArray;
        groundContents = inptGroundContents;
        messages = inptMessages;
        energy = inptEnergy;
        lastActionStatus = inptLastActionStatus;
        worldTime = inptWorldTime;
    }

    /**
     * Just read the raw data into an array of String.  Initialize the status field from line 0
     * <p>
     * LINE0: # of lines to be sent or one of: die, success, or End
     * LINE1: smell (food direction)
     * LINE2: inventory
     * LINE3: visual contents
     * LINE4: ground contents
     * LINE5: messages
     * LINE6: remaining energy
     * LINE7: lastActionStatus
     * LINE8: world time
     *
     * @param gridIn the reader connected to the server
     * @return the array of String representing the raw (unprocessed) sensory data starting with smell
     */
    // change from protected to private
     private String[] getRawSenseDataFromGrid(BufferedReader gridIn) { // return JsonArray
         JSONArray result = new JSONArray(); // fill in with all the data.
        try {
            LinkedList jsonArray = parse_info(gridIn.readLine()); // unpack the JsonArray.
            if (jsonArray.get(0).equals("CONTINUE")){ // Check status
                jsonArray.remove(0); // Remove status to make 0. Smell.
                for (int i = 0; i < jsonArray.size(); i++) {
                    if (i == 2 || i == 4) { // in the index of 2 or 4 we need to parse the JsonArray once more.

                      //  result.add(new LinkedList<String>(parseinfo(jsonArray.get(i).toString())));

                    }else {

                    }


                }
            }else {
                System.out.println("The final status: "+jsonArray.get(0));
                System.exit(1);
            }
        } catch (Exception e){
            e.getMessage();
        }
        return new String[3]; // Todo: Change this.
    }

    /**
     * This method is going to unpack the JsonArray
     * @param info It is a String of JsonArray.
     * @return JSONArray
     */
    private LinkedList parse_info(String info) {
        try {
            LinkedList<Object> objectLinkedList = new LinkedList<>();
            JSONParser jsonParser = new JSONParser();
            Object object = jsonParser.parse(info);
            JSONArray jsonArray = (JSONArray) object;
            objectLinkedList.addAll(jsonArray);

            // 0, 1, 6, 7 and 8 Stings
            // 4 and 2 Array of Stings
            // 3
            for (int i = 0; i < jsonArray.size(); i++) { // add all the elements.
                LinkedList seconddemcopy =  new LinkedList();
                JSONArray seconddem = (JSONArray) jsonArray.get(i);
                if (i == 2 || i == 4){
                    //  Array of Stings
                    for (int j = 0; j < seconddem.size(); j++) {
                        seconddemcopy.add(seconddem.get(j));
                    }
                    objectLinkedList.set(i,seconddemcopy);

                }else if (i == 3){
                    // Array of Array of Array of String


                }
            }


            JSONArray firstdem = (JSONArray) jsonArray.get(2);
            JSONArray seconddem = (JSONArray) jsonArray.get(2);
            JSONArray thireddem = (JSONArray) jsonArray.get(2);



            objectLinkedList.set(2, Collections.addAll(new LinkedList<>(), jsonArray.get(2))); // index 2
            objectLinkedList.set(4, Collections.addAll(new LinkedList<>(), jsonArray.get(4)));// index 4
            objectLinkedList.set(5, Collections.addAll(new LinkedList<>(), jsonArray.get(5)));// index 5
            objectLinkedList.add(3, Collections.addAll(new LinkedList<>(), seconddem));// index 3

            return objectLinkedList;
        } catch (ParseException e) {
            e.printStackTrace();
            return new LinkedList();
        }
    }

    /**
     * Perform any pre-processing, especially on the visual data
     * @param rawSenseData the raw unprocessed sense data
     */
    // change from protected to private
    private void initPreProcessedFields(String[] rawSenseData){  // take a JsonArray
        try {
            // smell
            this.smell = rawSenseData[0];

            // process inventory

            this.inventory = new ArrayList<>();

           // for(Character item :  addingdata)
            //    this.inventory.add(item);


            // visual field
            processRetinalField(rawSenseData[2]);
            // ground contents
            this.groundContents = new ArrayList<>();
            for(char item : rawSenseData[3].replaceAll("[\\(\"\\)\\s]+","").toCharArray())
                this.groundContents.add(item);
            // messages: *** Revisit this!! ***
            this.messages = rawSenseData[4];
            // energy
            this.energy = Integer.parseInt(rawSenseData[5]);
            // lastActionStatus
            this.lastActionStatus = rawSenseData[6].equalsIgnoreCase("ok");
            // world Time
            this.worldTime = Integer.parseInt(rawSenseData[7]);
        }catch (NullPointerException e){ e.getMessage(); }

    }

    /**
     * Process the single string representing all the rows and column contents of the visual sensory data
     * and convert it to a 2D array of Vectors of Strings.
     * @param info the visual sensory data string (structered as parenthesized list of lists) from server
     */
    // change from protected to private
    private void processRetinalField(String info) {  // take a JsonArray
        boolean seeAgent;
        StringTokenizer visTokens = new StringTokenizer(info, "(", true);
        visTokens.nextToken();
        for (int i = 6; i >= 0; i--) {              //iterate backwards so character printout displays correctly
            visTokens.nextToken();
            for (int j=0; j <=4; j++) {             //iterate through the columns
                seeAgent = false;
                int agentID = 0;
                visTokens.nextToken();
                char[] visArray = visTokens.nextToken().replaceAll("[\\(\"\\)\\s]+","").toCharArray();
                for(int k=0; k < visArray.length; k++){
                    if (visArray[k] >= 0 && visArray[k] <= 9){  // we have a digit
                        if (seeAgent){ // we're already processing an agent ID with possibly more than one digit
                            agentID = 10*agentID + (visArray[k] - '0');
                        } else {       // starting to process an agent ID
                            seeAgent = true;
                            agentID = (visArray[k] - '0');
                        }
                    } else {                                    // we have a non-agent ID
                        if (seeAgent){ // just finished processing agent ID -- record it
                            visualArray.get(i).get(j).add(String.valueOf(agentID));
                            seeAgent = false;
                            agentID = 0;
                        }
                        visualArray.get(i).get(j).add(String.valueOf(visArray[k])); // add the non-agent item
                    }
                }
            }
        }
    }

    /** Get the status of the agent in the simulation.  Refer to documentation and/or code
     * for definitive details but either is a number of raw lines to be subsequently processed
     * or is one of "DIE", "END", or "SUCCEED".  This will not typically be used by agents.
     * @return the status of the simulation
     */
    public String getStatus(){ return status; }

    /**
     * @return the string direction toward the food source as one of forward, back, left or right
     */
    public String getSmell(){ return smell; }

    /**
     * @return the current contents of the inventory as a list
     */
    public List<Character> getInventory(){ return inventory; }

    /**
     * @return the array of lists of strings representing what is currently within the field of view
     */
    public ArrayList<ArrayList<Vector<String>>> getVisualArray(){ return visualArray; }

    /**
     * @return the list of characters on the ground where the agent is standing
     */
    public List<Character> getGroundContents(){ return groundContents; }

    /**
     * NOTE: This may be out of sync with the Grid server and may need to be a list or something else.
     * @return the messages shouted or talked by other agents in the environment
     */
    public String getMessages(){ return messages; }

    /**
     * @return the remaining energy as indicated by the sensory information from the server
     */
    public int getEnergy(){ return energy; }

    /**
     * @return whether the last action was successful (true) or not (false)
     */
    public boolean getLastActionStatus(){ return lastActionStatus; }

    /**
     * @return the world time
     */
    public int getWorldTime(){ return worldTime; }

    /**
     * @return the array of Strings representing the raw sensory data
     */
    public String[] getRawSenseData(){return rawSenseData; }

    /**
     * Renders the visual information as semi-formatted string, making no allowances for
     * cells with more than one object
     */
    public void printVisualArray(){
        for ( ArrayList<Vector<String>> row : visualArray ){
            for ( Vector<String> cell : row ){
                if ( cell != null ){
                    System.out.print('[');
                    for (String s : cell)
                        System.out.print(s);
                    System.out.print(']');
                } else {
                    System.out.print("[ ]");
                }
            }
            System.out.println();
        }
        System.out.println();
    }
    
}
