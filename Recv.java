import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class Recv {

  private final static String QUEUE_NAME = "rabbitmq";
  public final static String QUEUE_SEND = "mensages";

  private Integer numberMensages;
  private ArrayList<String> nicknames;
  private ConnectionFactory factory;
  private Connection connection;
  private Connection connectionSend;
  private Channel channel;
  private Channel channelSend;

  public Recv() throws IOException, TimeoutException {
    super();
    this.nicknames = new ArrayList<String>();
    this.numberMensages = 0;

    factory = new ConnectionFactory();
    factory.setHost("localhost");
    connection = factory.newConnection();
    connectionSend = factory.newConnection();
    channel = connection.createChannel();
    channelSend = connectionSend.createChannel();
    channel.queueDeclare(QUEUE_NAME, false, false, false, null);
    channel.queuePurge(QUEUE_NAME);
    channel.basicQos(1);
    channelSend.queueDeclare(QUEUE_SEND, false, false, false, null);
    channelSend.queuePurge(QUEUE_SEND);
    channelSend.basicQos(1);
  }

  public static void main(String[] argv) throws Exception {
    Recv recv = new Recv();

    System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

    recv.postMensage();
    recv.getMensage();
  }

  /**
   * Grava uma mensagem de um usuario registrado
   */

  public void postMensage() {
    try {
      Consumer consumer = new DefaultConsumer(channel) {
        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
            throws IOException {
          try {
            ByteArrayInputStream in = new ByteArrayInputStream(body);
            ObjectInputStream objInput = new ObjectInputStream(in);
            MensageToServer mensageRecived;
            mensageRecived = (MensageToServer) objInput.readObject();
            objInput.close();
            in.close();

            nicknames.add(mensageRecived.getNickname());

            File myObj = new File(mensageRecived.getNickname() + '-' + numberMensages + ".serv");

            try {
              if (myObj.createNewFile()) {
                System.out.println("File created: " + myObj.getName());
              }

              FileWriter myWriter = new FileWriter(myObj);

              myWriter.write(mensageRecived.getMensage());
              myWriter.close();
              System.out.println("Successfully wrote to the file: " + myObj.getName());
            } catch (IOException e) {
              System.out.println("Erro durante a criação/escrita do arquivo: " + myObj.getName());
            }

            numberMensages++;
          } catch (ClassNotFoundException e1) {
            System.out.println(" [.] " + e1.toString());
          }
        }
      };

      channel.basicConsume(QUEUE_NAME, true, consumer);
    } catch (Exception e) {
      System.out.println(" [.] " + e.toString());
    }
  }

  /**
   * Envia as mensagens para os usuários
   */

  public void getMensage() {
    while (true) {

      // Creating a File object for actual directory
      File directoryPath = new File(".");

      FilenameFilter textFilefilter = new FilenameFilter() {
        public boolean accept(File dir, String name) {
          String lowercaseName = name.toLowerCase();
          if (lowercaseName.endsWith(".serv")) {
            return true;
          } else {
            return false;
          }
        }
      };

      // List of all the .serv files
      String filesList[] = directoryPath.list(textFilefilter);

      for (String file : filesList) {

        String nickname = null;
        Integer indexFile = 0;
        String pathname = null;

        String[] partsName = file.split("-");
        String tmp = partsName[1];

        String[] partsName2 = tmp.split(".serv");

        String tmp2 = partsName2[0];

        indexFile = Integer.valueOf(tmp2);

        nickname = partsName[0];
        pathname = file;

        if (pathname == null || nickname == null) {
          System.out.println("Arquivo não encontrado!");
          return;
        }

        File file2 = new File(pathname);

        try {
          StringBuilder contentFile = new StringBuilder();

          FileReader fileReader = new FileReader(file2);

          int j;

          while ((j = fileReader.read()) != -1) {
            contentFile.append((char) j);
          }

          fileReader.close();

          MensageToClient respMensage = new MensageToClient(nickname, contentFile.toString(), indexFile,
              this.nicknames.indexOf(nickname) == -1 ? 0 : this.nicknames.indexOf(nickname));

          ByteArrayOutputStream out = new ByteArrayOutputStream();

          ObjectOutputStream objOutput = new ObjectOutputStream(out);
          objOutput.writeObject(respMensage);
          objOutput.close();
          out.close();

          TimeUnit.MILLISECONDS.sleep(500);

          channelSend.basicPublish("", QUEUE_SEND, null, out.toByteArray());
        } catch (IOException e) {
          System.out.println(" [.] " + e.toString());
        } catch (InterruptedException e) {
          System.out.println(" [.] " + e.toString());
        }
      }
    }
  }
}
