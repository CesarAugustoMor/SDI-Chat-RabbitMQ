import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class Send implements AutoCloseable {

  public final static String QUEUE_NAME = "rabbitmq";
  public final static String QUEUE_RECIVE = "mensages";

  private Integer numberMensagesRecivers;
  private Channel channel;
  private Channel channelRecv;
  private ConnectionFactory factory;
  private Connection connection;
  private Connection connectionRecv;
  BasicProperties props;
  private Integer idInServer = -1;

  public Send() throws IOException, TimeoutException {
    super();
    factory = new ConnectionFactory();
    factory.setHost("localhost");
    connection = factory.newConnection();
    connectionRecv = factory.newConnection();
    channel = connection.createChannel();
    channelRecv = connectionRecv.createChannel();

    numberMensagesRecivers = 0;
  }

  public static void main(String[] argv) throws Exception {
    Send send = new Send();

    System.out.print("Digite o nickname: ");
    Scanner input = new Scanner(System.in);
    String nickname = input.nextLine();

    menu(input, send, nickname);

    input.close();
    send.close();
  }

  /**
   * menu de opções do cliente
   *
   * @param input    entrada do teclado
   * @param send     Objeto com a conecção com o servidor
   * @param nickname nome do cliente
   */
  public static void menu(Scanner input, Send send, String nickname) {

    int op = 1;
    while (op != 0) {
      System.out.println("Operações possiveis:");
      System.out.println("0 - Sair");
      System.out.println("1 - Enviar o arquivo");
      System.out.println("2 - Receber o arquivo");
      op = input.nextInt();

      switch (op) {
      case 1:
        System.out.println("Nome do arquivo a ser enviado (sem o '.chat'):");
        if (input.hasNext()) {
          input.nextLine();
        }
        String fileName = input.nextLine();

        send.sendMensage(fileName, nickname);
        break;
      case 2:
        send.reciveMensage(nickname);
        break;
      default:
        break;
      }

      try {
        Runtime.getRuntime().exec("cls");
      } catch (IOException e) {
      }
    }
  }

  /**
   * Envia uma mensagem para o servidor
   *
   * @param fileName nome do arquivo que a mensagem será lida
   * @param nickname nome do cliente
   */
  public void sendMensage(String fileName, String nickname) {
    File file = new File(fileName.trim() + ".chat");
    try {

      StringBuilder lines = new StringBuilder();

      Scanner scanFile = new Scanner(file);

      while (scanFile.hasNextLine()) {
        lines.append(scanFile.nextLine());
        if (scanFile.hasNextLine()) {
          lines.append("\n");
        }
      }

      scanFile.close();
      MensageToServer mensageSend = new MensageToServer(nickname, "sendMensage");
      mensageSend.setMensage(lines.toString());
      if (idInServer != -1) {
        mensageSend.setClient(idInServer);
      }

      ByteArrayOutputStream out = new ByteArrayOutputStream();

      ObjectOutputStream objOutput = new ObjectOutputStream(out);
      objOutput.writeObject(mensageSend);
      objOutput.close();
      channel.basicPublish("", QUEUE_NAME, null, out.toByteArray());
    } catch (FileNotFoundException e) {
      System.out.println("Arquivo " + file.getName() + " não encontrado!");
      System.out.println("Verifique o nome e tente novamente!");
    } catch (IOException e) {
      System.out.println("Erro no envio da mensagem.");
    }
  }

  /**
   * Recebe mensagens do servidor
   */
  public void reciveMensage(String nickname) {
    try {
      Consumer consumer = new DefaultConsumer(channelRecv) {
        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body)
            throws IOException {
          try {
            ByteArrayInputStream in = new ByteArrayInputStream(body);
            ObjectInputStream objInput = new ObjectInputStream(in);
            MensageToClient recivedResponse = (MensageToClient) objInput.readObject();
            objInput.close();
            in.close();

            File myObj = new File(
                nickname + '-' + recivedResponse.getIndex() + ".client" + recivedResponse.getClient());

            try {
              if (myObj.exists()) {
                return;
              }
              if (myObj.createNewFile()) {
                System.out.println("File created: " + myObj.getName());
              }

              FileWriter myWriter = new FileWriter(myObj);

              myWriter.write(recivedResponse.getMensage());
              myWriter.close();

              System.out.println("Sucesso em escrever no arquivo: " + myObj.getName());
            } catch (IOException e) {
              System.out.println("Erro ao salvar o arquivo: " + myObj.getName());
            }
          } catch (ClassNotFoundException e) {
            System.out.println(" [.] " + e.toString());
          }

          numberMensagesRecivers++;
        }
      };

      channelRecv.basicConsume(QUEUE_RECIVE, true, consumer);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void close() throws IOException, TimeoutException {
    channel.close();
    channelRecv.close();
    connection.close();
  }
}
