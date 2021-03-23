import java.io.Serializable;

public class MensageToServer implements Serializable {
  /**
   *
   */
  private static final long serialVersionUID = 8729163599395103785L;
  private String nickname;
  private String mensage;
  private Integer index;
  private String type;
  private Integer client;

  public MensageToServer(String nickname, String type) {
    super();
    this.nickname = nickname;
    this.type = type;
  }

  public String getNickname() {
    return nickname;
  }

  public void setMensage(String mensage) {
    this.mensage = mensage;
  }

  public String getMensage() {
    return mensage;
  }

  public String getType() {
    return type;
  }

  public Integer getIndex() {
    return index;
  }

  public void setIndex(Integer index) {
    this.index = index;
  }

  public Integer getClient() {
    return client;
  }

  public void setClient(Integer client) {
    this.client = client;
  }

  @Override
  public String toString() {
    StringBuilder str = new StringBuilder("Mensagem: ");
    str.append(this.mensage).append('\n').append("Nickname: ").append(this.nickname).append('\n').append("type: ")
        .append(this.type);
    return str.toString();
  }
}
