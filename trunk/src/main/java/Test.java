import com.google.protobuf.InvalidProtocolBufferException;
import com.mservice.momo.msg.MomoMessage;
import com.mservice.momo.msg.MomoProto;

/**
 * Created by nam on 5/7/14.
 */
public class Test {

    public Test() {
//        JsonObject obj = new JsonObject()
//                .putString("num", "39560940592923993952173717008920385357280975850777033456527135604936071983116408691334061595212483177749309999654111874791374329043735");
//
//        BigInteger num = (BigInteger) obj.getNumber("num");

//        Map<String, Object> map = new HashMap<>();
//
//        map.put("name", "hehe");
//        map.put("_id", "534ca63d13a621bafb425b7f");
//
//        JsonObject obj = new JsonObject(map);

        MomoMessage msg = new MomoMessage(MomoProto.MsgType.UPDATE_AGENT_INFO_VALUE, 0, 987568815,
                MomoProto.AgentInfo.newBuilder()
                        .setAddress("address")
                        .setCardId("cardId")
                        .setEmail("email")
                        .setName("name")
                        .build().toByteArray()
        );

        MomoProto.Register request;
        try {
            request = MomoProto.Register.parseFrom(msg.cmdBody);
            System.out.println("Name: " + request.getName());
        } catch (InvalidProtocolBufferException e) {
//            logger.error("InvalidProtocolBufferException", e);
            request = null;
        }

    }

    public static void main(String args[]) {
        new Test();

        /*MongoClient mongoClient = new MongoClient( "localhost" );
    // or
        MongoClient mongoClient = new MongoClient( "localhost" , 27017 );
// or, to connect to a replica set, with auto-discovery of the primary, supply a seed list of members
        MongoClient mongoClient = new MongoClient(Arrays.asList(new ServerAddress("localhost", 27017),
                new ServerAddress("localhost", 27018),
                new ServerAddress("localhost", 27019)));

        DB db = mongoClient.getDB( "mydb" );*/


    }
}
