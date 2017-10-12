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
    String status;
    String smell;
    List<Character> inventory;
    //List<Character>[][] visualArray = (List<Character>[][])new ArrayList[7][5];
    ArrayList<ArrayList<Vector<String>>> visualArray;
    List<Character> groundContents;
    LinkedList messages;
    int energy;
    boolean lastActionStatus;
    int worldTime;
    LinkedList rawSenseData;

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
                         List<Character> inptGroundContents, LinkedList inptMessages,
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
     private LinkedList getRawSenseDataFromGrid(BufferedReader gridIn) { // return JsonArray
         try {
             LinkedList jsonArray = parse_info(gridIn.readLine()); // unpack the JsonArray.
             if (jsonArray.get(0).equals("CONTINUE")){// Check status
                 jsonArray.remove(0); // Remove status to make the index 0 = Smell.
                 return jsonArray;
            }else {
                 System.out.println("The final status: "+jsonArray.get(0));
                 jsonArray.remove(0); // Remove status to make the index 0 = Smell.
                 System.exit(1);
             }
            } catch (Exception e){ e.getMessage(); }
         return new LinkedList();
    }

    /**
     * This method is going to unpack the JsonArray
     * @param info It is a String of JsonArray.
     * @return LinkedList
     */
    private LinkedList parse_info(String info) {
        try {
            LinkedList<Object> objectLinkedList = new LinkedList<>();
            JSONParser jsonParser = new JSONParser();
            Object object = jsonParser.parse(info);
            JSONArray jsonArray = (JSONArray) object;
            objectLinkedList.addAll(jsonArray); // add all the element from Json to LinkedList
            for (int i = 2; i <=5 ; i++) { // in case of 2 and 4 create a new Linked list that has all the elements
                if (i != 3){
                    JSONArray array = (JSONArray) jsonArray.get(i);
                    LinkedList f =  new LinkedList();
                    f.addAll(array);
                    objectLinkedList.set(i, f);
                }else { // in case 3 create LinkedList within a LinkedList within a LinkedList that has all the data.
                    JSONArray f = ((JSONArray) jsonArray.get(3)); // first dimension JsonArray
                    JSONArray s = (JSONArray) f.get(0); // second dimension JsonArray
                    LinkedList result =  new LinkedList();
                    LinkedList final_result =  new LinkedList();
                    for (int k = 0; k < s.size(); k++) {
                        result.add(new LinkedList<>());
                        JSONArray final_Array = (JSONArray) s.get(k); // third dimension JsonArray
                        LinkedList holder = (LinkedList) result.get(k); // get LinkedList just added.
                        holder.addAll(final_Array); // add all elements from third dimension JsonArray to a LinkedList.
                    }
                    final_result.add(result);
                    objectLinkedList.set(3, final_result);
                }
            }
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
    private void initPreProcessedFields(LinkedList rawSenseData){  // take a JsonArray
        try {
            // smell
            this.smell = rawSenseData.get(0).toString();
            // process inventory
            this.inventory = (List<Character>) rawSenseData.get(1);
            // visual field
            processRetinalField((LinkedList) rawSenseData.get(2));
            // ground contents
            this.groundContents = (List<Character>) rawSenseData.get(3);
            // messages: *** Revisit this!! ***
            this.messages = (LinkedList) rawSenseData.get(4);
            // energy
            this.energy = Integer.parseInt(rawSenseData.get(5).toString());
            // lastActionStatus
            this.lastActionStatus = rawSenseData.get(6).equals("ok".toLowerCase());
            // world Time
            this.worldTime = Integer.parseInt(rawSenseData.get(7).toString());
        }catch (NullPointerException e){ e.getMessage(); }

    }

    /**
     * Process the single string representing all the rows and column contents of the visual sensory data
     * and convert it to a 2D array of Vectors of Strings.
     * @param info the visual sensory data string (structered as parenthesized list of lists) from server
     */
    // change from protected to private
    // Todo: Start fixing this:
    private void processRetinalField(LinkedList info) {  // take a JsonArray
        boolean seeAgent;
        info = (LinkedList) info.get(0);
        LinkedList s = (LinkedList) info.get(3);
        for (int i = 6; i >= 0; i--) {              //iterate backwards so character printout displays correctly
            for (int j=0; j <=4; j++) {             //iterate through the columns
                seeAgent = false;
                int agentID = 0;
                LinkedList t = (LinkedList) s.get(i);
                Integer id_num = Integer.parseInt((String) t.get(j));
                    if (id_num >= 0 && id_num <= 9) {  // we have a digit
                        if (seeAgent) { // we're already processing an agent ID with possibly more than one digit
                            agentID = 10 * agentID + (id_num - '0');
                        } else {       // starting to process an agent ID
                            seeAgent = true;
                            agentID = (id_num - '0');
                        }
                    } else {                                    // we have a non-agent ID
                        if (seeAgent){ // just finished processing agent ID -- record it
                            visualArray.get(i).get(j).add(String.valueOf(agentID));
                            seeAgent = false;
                            agentID = 0;
                        }
                        visualArray.get(i).get(j).add(String.valueOf(t.get(j))); // add the non-agent item
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
    public LinkedList getMessages(){ return messages; }

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
    public LinkedList getRawSenseData(){return rawSenseData; }

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
