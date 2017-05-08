import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.logging.*;

/**
 * A class representing a child processes 
 * @author Karan Bhargava
 *
 */
public class Child implements Runnable {

	static final int END_TIME = 3000; //30 second run time
	int start_time;

	// create a byteBuffer and a pipe 
	ByteBuffer byte_buffer; //Given a direct byte buffer,Java VM will make a best effort to perform native I/O operations directly upon it
	Pipe pipe; // A pair of channels that implements a unidirectional pipe.

	int name_of_child;

	/**
	 * A method to create a child that can use a pipe to send messages to the parent
	 * Behavior depends on what child it is
	 * Child 5, for instance, waits for user input via stdin
	 * @param name - the child name
	 * @param start - the start time
	 * @param p - the pipe
	 */
	Child(int name, int start, Pipe p) {
		name_of_child = name;
		start_time = start;
		pipe = p;
	}

	@Override
	/**
	 * An overwritten method of the runnable class to make 
	 * a child runnable
	 */
	public void run() {
		int message_num = 0; //message number
		String message; //the message
		String time_stamp; //time stamp of the process
		float current_time; //the current time

		while(System.currentTimeMillis() - start_time < END_TIME) { // all processes stop at time 30
			if(name_of_child != 4) { // 5th child takes in input via stdin
				Random random = new Random(); 
				int sleep_time = random.nextInt(3) * 1000; //make the process sleep for 0 to 2 seconds
				try {
					Thread.sleep(sleep_time); //try making the thread sleep
				} catch (InterruptedException e){ //catch any exception
					e.printStackTrace();
				}

				//format the message
				current_time = (float) ((System.currentTimeMillis() - start_time) / 1000.0);
				time_stamp = String.format("%06.3f", current_time);
				message_num++;
				message = "0:" + time_stamp + ": Child " + (name_of_child + 1) + " message " + message_num + "\n";
			} else {
				System.out.println("Please enter a message"); //take in input via stdin
				Scanner in = new Scanner(System.in);

				//format the message
				current_time = (float) ((System.currentTimeMillis() - start_time) / 1000.0);
				time_stamp = String.format("%06.3f", current_time);
				message_num++;
				message = "0:" + time_stamp + ": Child " + (name_of_child + 1) + in.nextLine()  + message_num + "\n";

				in.close(); //close the scanner
			}

			// try writing to the pipe
			try {
				byte_buffer.clear();
				byte_buffer.put(message.getBytes());
				byte_buffer.flip();

				while(byte_buffer.hasRemaining()) { 
					pipe.sink().write(byte_buffer); //write to pipe as long as there's something in the buffer
				}

				byte_buffer.clear(); //clear it for next usage

			} catch (IOException e) {
				Logger.getLogger(Child.class.getName()).log(Level.SEVERE, null, e);
			}
		}
	}
}