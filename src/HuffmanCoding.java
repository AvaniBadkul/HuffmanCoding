package huffman;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * This class contains methods which, when used together, perform the
 * entire Huffman Coding encoding and decoding process
 * 
 * @author Ishaan Ivaturi
 * @author Prince Rawal
 */
public class HuffmanCoding {
    private String fileName;
    private ArrayList<CharFreq> sortedCharFreqList;
    private TreeNode huffmanRoot;
    private String[] encodings;

    /**
     * Constructor used by the driver, sets filename
     * DO NOT EDIT
     * @param f The file we want to encode
     */
    public HuffmanCoding(String f) { 
        fileName = f; 
    }

    /**
     * Reads from filename character by character, and sets sortedCharFreqList
     * to a new ArrayList of CharFreq objects with frequency > 0, sorted by frequency
     */
    public void makeSortedList() {
        StdIn.setFile(fileName);

        int[] freq = new int[128];
        int totalChars = 0;
    
        while (StdIn.hasNextChar()) {
            char c = StdIn.readChar();
            freq[c]++;
            totalChars++;
        }
    
        ArrayList<CharFreq> charFreqList = new ArrayList<>();
        for (int i = 0; i < 128; i++) {
            if (freq[i] > 0) {
                charFreqList.add(new CharFreq((char) i, (double) freq[i] / totalChars));
            }
        }
    
        if (charFreqList.size() == 1) {
            charFreqList.add(new CharFreq((char) ((charFreqList.get(0).getCharacter() + 1) % 128), 0.0));
        }
    
        Collections.sort(charFreqList);
    
        sortedCharFreqList = charFreqList;        
	/* Your code goes here */
    }

    /**
     * Uses sortedCharFreqList to build a huffman coding tree, and stores its root
     * in huffmanRoot
     */
    public void makeTree() {
        ArrayList<TreeNode> nodes = new ArrayList<>();
        for (CharFreq cf : sortedCharFreqList) {
            nodes.add(new TreeNode(cf, null, null));
        }
    
        // Build the tree by merging nodes
        while (nodes.size() > 1) {
            // Sort nodes by frequency (smallest first)
            Collections.sort(nodes, (n1, n2) -> Double.compare(n1.getData().getProbOcc(), n2.getData().getProbOcc()));
    
            // Merge the two smallest nodes into a new parent node
            TreeNode left = nodes.remove(0);
            TreeNode right = nodes.remove(0);
            CharFreq parentData = new CharFreq(null, left.getData().getProbOcc() + right.getData().getProbOcc());
            TreeNode parent = new TreeNode(parentData, left, right);
    
            // Add the new parent node to the list of nodes
            nodes.add(parent);
        }
    
        // The last node in the list is the root of the tree
        huffmanRoot = nodes.get(0);
	/* Your code goes here */
    }

    /**
     * Uses huffmanRoot to create a string array of size 128, where each
     * index in the array contains that ASCII character's bitstring encoding. Characters not
     * present in the huffman coding tree should have their spots in the array left null.
     * Set encodings to this array.
     */
    public void makeEncodings() {
        encodings = new String[128];
        makeEncodingsHelper(huffmanRoot, "");
	/* Your code goes here */
    }
    private void makeEncodingsHelper(TreeNode node, String encoding) {
        if (node.getLeft() == null && node.getRight() == null) {
            encodings[node.getData().getCharacter()] = encoding;
            return;
        }
        makeEncodingsHelper(node.getLeft(), encoding + "0");
        makeEncodingsHelper(node.getRight(), encoding + "1");
    }

    /**
     * Using encodings and filename, this method makes use of the writeBitString method
     * to write the final encoding of 1's and 0's to the encoded file.
     * 
     * @param encodedFile The file name into which the text file is to be encoded
     */
    public void encode(String encodedFile) {
        StdIn.setFile(fileName);

        // create a StringBuilder to store the compressed text
        StringBuilder compressedText = new StringBuilder();
        
        // read each character from the input file and append its encoding to the compressed text
        while (StdIn.hasNextChar()) {
            char c = StdIn.readChar();
            compressedText.append(encodings[c]);
        }
        
        // write the compressed text to the output file using the writeBitString method
        writeBitString(encodedFile, compressedText.toString());
	/* Your code goes here */
    }
    
    /**
     * Writes a given string of 1's and 0's to the given file byte by byte
     * and NOT as characters of 1 and 0 which take up 8 bits each
     * DO NOT EDIT
     * 
     * @param filename The file to write to (doesn't need to exist yet)
     * @param bitString The string of 1's and 0's to write to the file in bits
     */
    public static void writeBitString(String filename, String bitString) {
        byte[] bytes = new byte[bitString.length() / 8 + 1];
        int bytesIndex = 0, byteIndex = 0, currentByte = 0;

        // Pad the string with initial zeroes and then a one in order to bring
        // its length to a multiple of 8. When reading, the 1 signifies the
        // end of padding.
        int padding = 8 - (bitString.length() % 8);
        String pad = "";
        for (int i = 0; i < padding-1; i++) pad = pad + "0";
        pad = pad + "1";
        bitString = pad + bitString;

        // For every bit, add it to the right spot in the corresponding byte,
        // and store bytes in the array when finished
        for (char c : bitString.toCharArray()) {
            if (c != '1' && c != '0') {
                System.out.println("Invalid characters in bitstring");
                return;
            }

            if (c == '1') currentByte += 1 << (7-byteIndex);
            byteIndex++;
            
            if (byteIndex == 8) {
                bytes[bytesIndex] = (byte) currentByte;
                bytesIndex++;
                currentByte = 0;
                byteIndex = 0;
            }
        }
        
        // Write the array of bytes to the provided file
        try {
            FileOutputStream out = new FileOutputStream(filename);
            out.write(bytes);
            out.close();
        }
        catch(Exception e) {
            System.err.println("Error when writing to file!");
        }
    }

    /**
     * Using a given encoded file name, this method makes use of the readBitString method 
     * to convert the file into a bit string, then decodes the bit string using the 
     * tree, and writes it to a decoded file. 
     * 
     * @param encodedFile The file which has already been encoded by encode()
     * @param decodedFile The name of the new file we want to decode into
     */
    public void decode(String encodedFile, String decodedFile) {
        StdOut.setFile(decodedFile);

        String bitString = readBitString(encodedFile);
        StringBuilder decodedText = new StringBuilder();
    
        TreeNode currentNode = huffmanRoot;
        for (int i = 0; i < bitString.length(); i++) {
            char bit = bitString.charAt(i);
            if (bit == '0') {
                currentNode = currentNode.getLeft();
            } else {
                currentNode = currentNode.getRight();
            }
    
            CharFreq data = currentNode.getData();
            if (data.getCharacter() != null) {
                decodedText.append(data.getCharacter());
                currentNode = huffmanRoot;
            }
        }
    
        StdOut.print(decodedText.toString());
	/* Your code goes here */
    }

    /**
     * Reads a given file byte by byte, and returns a string of 1's and 0's
     * representing the bits in the file
     * DO NOT EDIT
     * 
     * @param filename The encoded file to read from
     * @return String of 1's and 0's representing the bits in the file
     */
    public static String readBitString(String filename) {
        String bitString = "";
        
        try {
            FileInputStream in = new FileInputStream(filename);
            File file = new File(filename);

            byte bytes[] = new byte[(int) file.length()];
            in.read(bytes);
            in.close();
            
            // For each byte read, convert it to a binary string of length 8 and add it
            // to the bit string
            for (byte b : bytes) {
                bitString = bitString + 
                String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
            }

            // Detect the first 1 signifying the end of padding, then remove the first few
            // characters, including the 1
            for (int i = 0; i < 8; i++) {
                if (bitString.charAt(i) == '1') return bitString.substring(i+1);
            }
            
            return bitString.substring(8);
        }
        catch(Exception e) {
            System.out.println("Error while reading file!");
            return "";
        }
    }

    /*
     * Getters used by the driver. 
     * DO NOT EDIT or REMOVE
     */

    public String getFileName() { 
        return fileName; 
    }

    public ArrayList<CharFreq> getSortedCharFreqList() { 
        return sortedCharFreqList; 
    }

    public TreeNode getHuffmanRoot() { 
        return huffmanRoot; 
    }

    public String[] getEncodings() { 
        return encodings; 
    }
}
