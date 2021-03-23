import java.io.Serializable;

public class MensageToClient implements Serializable {
  /**
   *
   */
  private static final long serialVersionUID = 7294780571722765875L;
  private String nickname;
  private String mensage;
  private Integer index;
  private Integer client;

  public MensageToClient(String nickname, String mensage, Integer index, Integer client) {
    super();
    this.mensage = mensage;
    this.nickname = nickname;
    this.index = index;
    this.client = client;
  }

  public String getNickname() {
    return nickname;
  }

  public String getMensage() {
    return mensage;
  }

  public Integer getIndex() {
    return index;
  }

  public Integer getClient() {
    return client;
  }

  @Override
  public String toString() {
    StringBuilder str = new StringBuilder("Mensagem: ");
    str.append(this.mensage).append('\n').append("Nickname: ").append(this.nickname).append('\n').append("index: ")
        .append(this.index).append('\n').append("Client: ").append(this.client).append('\n');
    return str.toString();
  }

}
