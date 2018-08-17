/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Graylog.  If not,
 * see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.inputs.random.generators;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;
import com.spatial4j.core.context.SpatialContext;
import com.spatial4j.core.context.SpatialContextFactory;
import com.spatial4j.core.shape.Point;
import com.spatial4j.core.shape.Rectangle;
import java.util.Collections;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static java.util.Objects.requireNonNull;
import static org.graylog2.inputs.random.generators.FakeHttpRawMessageGenerator.GeneratorState.Method.DELETE;
import static org.graylog2.inputs.random.generators.FakeHttpRawMessageGenerator.GeneratorState.Method.GET;
import static org.graylog2.inputs.random.generators.FakeHttpRawMessageGenerator.GeneratorState.Method.POST;
import static org.graylog2.inputs.random.generators.FakeHttpRawMessageGenerator.GeneratorState.Method.PUT;

public class FakeHttpRawMessageGenerator {

  private static final Random RANDOM = new Random();
  private static final int MAX_WEIGHT = 50;
  private static final SpatialContext SPATIAL_CONTEXT = SpatialContextFactory.makeSpatialContext(Collections.emptyMap(), null);

  private static final List<CountryBoundingBox> BOUNDING_BOXES = ImmutableList.of(
      new CountryBoundingBox("AF", 1, SPATIAL_CONTEXT.makeRectangle(60.5284298033, 75.1580277851, 29.318572496, 38.4862816432)),
      new CountryBoundingBox("AO", 1, SPATIAL_CONTEXT.makeRectangle(11.6400960629, 24.0799052263, -17.9306364885, -4.43802336998)),
      new CountryBoundingBox("AL", 1, SPATIAL_CONTEXT.makeRectangle(19.3044861183, 21.0200403175, 39.624997667, 42.6882473822)),
      new CountryBoundingBox("AE", 1, SPATIAL_CONTEXT.makeRectangle(51.5795186705, 56.3968473651, 22.4969475367, 26.055464179)),
      new CountryBoundingBox("AR", 1, SPATIAL_CONTEXT.makeRectangle(-73.4154357571, -53.628348965, -55.25, -21.8323104794)),
      new CountryBoundingBox("AM", 1, SPATIAL_CONTEXT.makeRectangle(43.5827458026, 46.5057198423, 38.7412014837, 41.2481285671)),
      new CountryBoundingBox("AQ", 1, SPATIAL_CONTEXT.makeRectangle(-180.0, 180.0, -90.0, -63.2706604895)),
      new CountryBoundingBox("TF", 1, SPATIAL_CONTEXT.makeRectangle(68.72, 70.56, -49.775, -48.625)),
      new CountryBoundingBox("AU", 5, SPATIAL_CONTEXT.makeRectangle(113.338953078, 153.569469029, -43.6345972634, -10.6681857235)),
      new CountryBoundingBox("AT", 1, SPATIAL_CONTEXT.makeRectangle(9.47996951665, 16.9796667823, 46.4318173285, 49.0390742051)),
      new CountryBoundingBox("AZ", 1, SPATIAL_CONTEXT.makeRectangle(44.7939896991, 50.3928210793, 38.2703775091, 41.8606751572)),
      new CountryBoundingBox("BI", 1, SPATIAL_CONTEXT.makeRectangle(29.0249263852, 30.752262811, -4.49998341229, -2.34848683025)),
      new CountryBoundingBox("BE", 10, SPATIAL_CONTEXT.makeRectangle(2.51357303225, 6.15665815596, 49.5294835476, 51.4750237087)),
      new CountryBoundingBox("BJ", 1, SPATIAL_CONTEXT.makeRectangle(0.772335646171, 3.79711225751, 6.14215770103, 12.2356358912)),
      new CountryBoundingBox("BF", 1, SPATIAL_CONTEXT.makeRectangle(-5.47056494793, 2.17710778159, 9.61083486576, 15.1161577418)),
      new CountryBoundingBox("BD", 1, SPATIAL_CONTEXT.makeRectangle(88.0844222351, 92.6727209818, 20.670883287, 26.4465255803)),
      new CountryBoundingBox("BG", 1, SPATIAL_CONTEXT.makeRectangle(22.3805257504, 28.5580814959, 41.2344859889, 44.2349230007)),
      new CountryBoundingBox("BS", 1, SPATIAL_CONTEXT.makeRectangle(-78.98, -77.0, 23.71, 27.04)),
      new CountryBoundingBox("BA", 1, SPATIAL_CONTEXT.makeRectangle(15.7500260759, 19.59976, 42.65, 45.2337767604)),
      new CountryBoundingBox("BY", 1, SPATIAL_CONTEXT.makeRectangle(23.1994938494, 32.6936430193, 51.3195034857, 56.1691299506)),
      new CountryBoundingBox("BZ", 1, SPATIAL_CONTEXT.makeRectangle(-89.2291216703, -88.1068129138, 15.8869375676, 18.4999822047)),
      new CountryBoundingBox("BO", 1, SPATIAL_CONTEXT.makeRectangle(-69.5904237535, -57.4983711412, -22.8729187965, -9.76198780685)),
      new CountryBoundingBox("BR", 6, SPATIAL_CONTEXT.makeRectangle(-73.9872354804, -34.7299934555, -33.7683777809, 5.24448639569)),
      new CountryBoundingBox("BN", 1, SPATIAL_CONTEXT.makeRectangle(114.204016555, 115.450710484, 4.007636827, 5.44772980389)),
      new CountryBoundingBox("BT", 1, SPATIAL_CONTEXT.makeRectangle(88.8142484883, 92.1037117859, 26.7194029811, 28.2964385035)),
      new CountryBoundingBox("BW", 1, SPATIAL_CONTEXT.makeRectangle(19.8954577979, 29.4321883481, -26.8285429827, -17.6618156877)),
      new CountryBoundingBox("CF", 1, SPATIAL_CONTEXT.makeRectangle(14.4594071794, 27.3742261085, 2.2676396753, 11.1423951278)),
      new CountryBoundingBox("CA", 15, SPATIAL_CONTEXT.makeRectangle(-140.99778, -52.6480987209, 41.6751050889, 83.23324)),
      new CountryBoundingBox("CH", 1, SPATIAL_CONTEXT.makeRectangle(6.02260949059, 10.4427014502, 45.7769477403, 47.8308275417)),
      new CountryBoundingBox("CL", 1, SPATIAL_CONTEXT.makeRectangle(-75.6443953112, -66.95992, -55.61183, -17.5800118954)),
      new CountryBoundingBox("CN", 30, SPATIAL_CONTEXT.makeRectangle(73.6753792663, 135.026311477, 18.197700914, 53.4588044297)),
      new CountryBoundingBox("CI", 1, SPATIAL_CONTEXT.makeRectangle(-8.60288021487, -2.56218950033, 4.33828847902, 10.5240607772)),
      new CountryBoundingBox("CM", 1, SPATIAL_CONTEXT.makeRectangle(8.48881554529, 16.0128524106, 1.72767263428, 12.8593962671)),
      new CountryBoundingBox("CD", 1, SPATIAL_CONTEXT.makeRectangle(12.1823368669, 31.1741492042, -13.2572266578, 5.25608775474)),
      new CountryBoundingBox("CG", 1, SPATIAL_CONTEXT.makeRectangle(11.0937728207, 18.4530652198, -5.03798674888, 3.72819651938)),
      new CountryBoundingBox("CO", 1, SPATIAL_CONTEXT.makeRectangle(-78.9909352282, -66.8763258531, -4.29818694419, 12.4373031682)),
      new CountryBoundingBox("CR", 1, SPATIAL_CONTEXT.makeRectangle(-85.94172543, -82.5461962552, 8.22502798099, 11.2171192489)),
      new CountryBoundingBox("CU", 1, SPATIAL_CONTEXT.makeRectangle(-84.9749110583, -74.1780248685, 19.8554808619, 23.1886107447)),
      new CountryBoundingBox("CY", 1, SPATIAL_CONTEXT.makeRectangle(32.2566671079, 34.0048808123, 34.5718694118, 35.1731247015)),
      new CountryBoundingBox("CZ", 1, SPATIAL_CONTEXT.makeRectangle(12.2401111182, 18.8531441586, 48.5553052842, 51.1172677679)),
      new CountryBoundingBox("DE", 25, SPATIAL_CONTEXT.makeRectangle(5.98865807458, 15.0169958839, 47.3024876979, 54.983104153)),
      new CountryBoundingBox("DJ", 1, SPATIAL_CONTEXT.makeRectangle(41.66176, 43.3178524107, 10.9268785669, 12.6996385767)),
      new CountryBoundingBox("DK", 5, SPATIAL_CONTEXT.makeRectangle(8.08997684086, 12.6900061378, 54.8000145534, 57.730016588)),
      new CountryBoundingBox("DO", 1, SPATIAL_CONTEXT.makeRectangle(-71.9451120673, -68.3179432848, 17.598564358, 19.8849105901)),
      new CountryBoundingBox("DZ", 1, SPATIAL_CONTEXT.makeRectangle(-8.68439978681, 11.9995056495, 19.0573642034, 37.1183806422)),
      new CountryBoundingBox("EC", 1, SPATIAL_CONTEXT.makeRectangle(-80.9677654691, -75.2337227037, -4.95912851321, 1.3809237736)),
      new CountryBoundingBox("EG", 1, SPATIAL_CONTEXT.makeRectangle(24.70007, 36.86623, 22.0, 31.58568)),
      new CountryBoundingBox("ER", 1, SPATIAL_CONTEXT.makeRectangle(36.3231889178, 43.0812260272, 12.4554157577, 17.9983074)),
      new CountryBoundingBox("ES", 1, SPATIAL_CONTEXT.makeRectangle(-9.39288367353, 3.03948408368, 35.946850084, 43.7483377142)),
      new CountryBoundingBox("EE", 1, SPATIAL_CONTEXT.makeRectangle(23.3397953631, 28.1316992531, 57.4745283067, 59.6110903998)),
      new CountryBoundingBox("ET", 1, SPATIAL_CONTEXT.makeRectangle(32.95418, 47.78942, 3.42206, 14.95943)),
      new CountryBoundingBox("FI", 5, SPATIAL_CONTEXT.makeRectangle(20.6455928891, 31.5160921567, 59.846373196, 70.1641930203)),
      new CountryBoundingBox("FJ", 1, SPATIAL_CONTEXT.makeRectangle(-180.0, 180.0, -18.28799, -16.0208822567)),
      new CountryBoundingBox("FK", 1, SPATIAL_CONTEXT.makeRectangle(-61.2, -57.75, -52.3, -51.1)),
      new CountryBoundingBox("FR", 1, SPATIAL_CONTEXT.makeRectangle(-54.5247541978, 9.56001631027, 2.05338918702, 51.1485061713)),
      new CountryBoundingBox("GA", 1, SPATIAL_CONTEXT.makeRectangle(8.79799563969, 14.4254557634, -3.97882659263, 2.32675751384)),
      new CountryBoundingBox("GB", 1, SPATIAL_CONTEXT.makeRectangle(-7.57216793459, 1.68153079591, 49.959999905, 58.6350001085)),
      new CountryBoundingBox("GE", 1, SPATIAL_CONTEXT.makeRectangle(39.9550085793, 46.6379081561, 41.0644446885, 43.553104153)),
      new CountryBoundingBox("GH", 1, SPATIAL_CONTEXT.makeRectangle(-3.24437008301, 1.0601216976, 4.71046214438, 11.0983409693)),
      new CountryBoundingBox("GN", 1, SPATIAL_CONTEXT.makeRectangle(-15.1303112452, -7.83210038902, 7.3090373804, 12.5861829696)),
      new CountryBoundingBox("GM", 1, SPATIAL_CONTEXT.makeRectangle(-16.8415246241, -13.8449633448, 13.1302841252, 13.8764918075)),
      new CountryBoundingBox("GW", 1, SPATIAL_CONTEXT.makeRectangle(-16.6774519516, -13.7004760401, 11.0404116887, 12.6281700708)),
      new CountryBoundingBox("GQ", 1, SPATIAL_CONTEXT.makeRectangle(9.3056132341, 11.285078973, 1.01011953369, 2.28386607504)),
      new CountryBoundingBox("GR", 1, SPATIAL_CONTEXT.makeRectangle(20.1500159034, 26.6041955909, 34.9199876979, 41.8269046087)),
      new CountryBoundingBox("GL", 1, SPATIAL_CONTEXT.makeRectangle(-73.297, -12.20855, 60.03676, 83.64513)),
      new CountryBoundingBox("GT", 1, SPATIAL_CONTEXT.makeRectangle(-92.2292486234, -88.2250227526, 13.7353376327, 17.8193260767)),
      new CountryBoundingBox("GY", 1, SPATIAL_CONTEXT.makeRectangle(-61.4103029039, -56.5393857489, 1.26808828369, 8.36703481692)),
      new CountryBoundingBox("HN", 1, SPATIAL_CONTEXT.makeRectangle(-89.3533259753, -83.147219001, 12.9846857772, 16.0054057886)),
      new CountryBoundingBox("HR", 1, SPATIAL_CONTEXT.makeRectangle(13.6569755388, 19.3904757016, 42.47999136, 46.5037509222)),
      new CountryBoundingBox("HT", 1, SPATIAL_CONTEXT.makeRectangle(-74.4580336168, -71.6248732164, 18.0309927434, 19.9156839055)),
      new CountryBoundingBox("HU", 1, SPATIAL_CONTEXT.makeRectangle(16.2022982113, 22.710531447, 45.7594811061, 48.6238540716)),
      new CountryBoundingBox("ID", 1, SPATIAL_CONTEXT.makeRectangle(95.2930261576, 141.03385176, -10.3599874813, 5.47982086834)),
      new CountryBoundingBox("IN", 1, SPATIAL_CONTEXT.makeRectangle(68.1766451354, 97.4025614766, 7.96553477623, 35.4940095078)),
      new CountryBoundingBox("IE", 1, SPATIAL_CONTEXT.makeRectangle(-9.97708574059, -6.03298539878, 51.6693012559, 55.1316222195)),
      new CountryBoundingBox("IR", 1, SPATIAL_CONTEXT.makeRectangle(44.1092252948, 63.3166317076, 25.0782370061, 39.7130026312)),
      new CountryBoundingBox("IQ", 1, SPATIAL_CONTEXT.makeRectangle(38.7923405291, 48.5679712258, 29.0990251735, 37.3852635768)),
      new CountryBoundingBox("IS", 1, SPATIAL_CONTEXT.makeRectangle(-24.3261840479, -13.609732225, 63.4963829617, 66.5267923041)),
      new CountryBoundingBox("IL", 1, SPATIAL_CONTEXT.makeRectangle(34.2654333839, 35.8363969256, 29.5013261988, 33.2774264593)),
      new CountryBoundingBox("IT", 1, SPATIAL_CONTEXT.makeRectangle(6.7499552751, 18.4802470232, 36.619987291, 47.1153931748)),
      new CountryBoundingBox("JM", 1, SPATIAL_CONTEXT.makeRectangle(-78.3377192858, -76.1996585761, 17.7011162379, 18.5242184514)),
      new CountryBoundingBox("JO", 1, SPATIAL_CONTEXT.makeRectangle(34.9226025734, 39.1954683774, 29.1974946152, 33.3786864284)),
      new CountryBoundingBox("JP", 1, SPATIAL_CONTEXT.makeRectangle(129.408463169, 145.543137242, 31.0295791692, 45.5514834662)),
      new CountryBoundingBox("KZ", 1, SPATIAL_CONTEXT.makeRectangle(46.4664457538, 87.3599703308, 40.6623245306, 55.3852501491)),
      new CountryBoundingBox("KE", 1, SPATIAL_CONTEXT.makeRectangle(33.8935689697, 41.8550830926, -4.67677, 5.506)),
      new CountryBoundingBox("KG", 1, SPATIAL_CONTEXT.makeRectangle(69.464886916, 80.2599902689, 39.2794632025, 43.2983393418)),
      new CountryBoundingBox("KH", 1, SPATIAL_CONTEXT.makeRectangle(102.3480994, 107.614547968, 10.4865436874, 14.5705838078)),
      new CountryBoundingBox("KR", 1, SPATIAL_CONTEXT.makeRectangle(126.117397903, 129.468304478, 34.3900458847, 38.6122429469)),
      new CountryBoundingBox("KW", 1, SPATIAL_CONTEXT.makeRectangle(46.5687134133, 48.4160941913, 28.5260627304, 30.0590699326)),
      new CountryBoundingBox("LA", 1, SPATIAL_CONTEXT.makeRectangle(100.115987583, 107.564525181, 13.88109101, 22.4647531194)),
      new CountryBoundingBox("LB", 1, SPATIAL_CONTEXT.makeRectangle(35.1260526873, 36.6117501157, 33.0890400254, 34.6449140488)),
      new CountryBoundingBox("LR", 1, SPATIAL_CONTEXT.makeRectangle(-11.4387794662, -7.53971513511, 4.35575511313, 8.54105520267)),
      new CountryBoundingBox("LY", 1, SPATIAL_CONTEXT.makeRectangle(9.31941084152, 25.16482, 19.58047, 33.1369957545)),
      new CountryBoundingBox("LK", 1, SPATIAL_CONTEXT.makeRectangle(79.6951668639, 81.7879590189, 5.96836985923, 9.82407766361)),
      new CountryBoundingBox("LS", 1, SPATIAL_CONTEXT.makeRectangle(26.9992619158, 29.3251664568, -30.6451058896, -28.6475017229)),
      new CountryBoundingBox("LT", 1, SPATIAL_CONTEXT.makeRectangle(21.0558004086, 26.5882792498, 53.9057022162, 56.3725283881)),
      new CountryBoundingBox("LU", 1, SPATIAL_CONTEXT.makeRectangle(5.67405195478, 6.24275109216, 49.4426671413, 50.1280516628)),
      new CountryBoundingBox("LV", 1, SPATIAL_CONTEXT.makeRectangle(21.0558004086, 28.1767094256, 55.61510692, 57.9701569688)),
      new CountryBoundingBox("MA", 1, SPATIAL_CONTEXT.makeRectangle(-17.0204284327, -1.12455115397, 21.4207341578, 35.7599881048)),
      new CountryBoundingBox("MD", 1, SPATIAL_CONTEXT.makeRectangle(26.6193367856, 30.0246586443, 45.4882831895, 48.4671194525)),
      new CountryBoundingBox("MG", 1, SPATIAL_CONTEXT.makeRectangle(43.2541870461, 50.4765368996, -25.6014344215, -12.0405567359)),
      new CountryBoundingBox("MX", 1, SPATIAL_CONTEXT.makeRectangle(-117.12776, -86.811982388, 14.5388286402, 32.72083)),
      new CountryBoundingBox("MK", 1, SPATIAL_CONTEXT.makeRectangle(20.46315, 22.9523771502, 40.8427269557, 42.3202595078)),
      new CountryBoundingBox("ML", 1, SPATIAL_CONTEXT.makeRectangle(-12.1707502914, 4.27020999514, 10.0963607854, 24.9745740829)),
      new CountryBoundingBox("MM", 1, SPATIAL_CONTEXT.makeRectangle(92.3032344909, 101.180005324, 9.93295990645, 28.335945136)),
      new CountryBoundingBox("ME", 1, SPATIAL_CONTEXT.makeRectangle(18.45, 20.3398, 41.87755, 43.52384)),
      new CountryBoundingBox("MN", 1, SPATIAL_CONTEXT.makeRectangle(87.7512642761, 119.772823928, 41.5974095729, 52.0473660345)),
      new CountryBoundingBox("MZ", 1, SPATIAL_CONTEXT.makeRectangle(30.1794812355, 40.7754752948, -26.7421916643, -10.3170960425)),
      new CountryBoundingBox("MR", 1, SPATIAL_CONTEXT.makeRectangle(-17.0634232243, -4.92333736817, 14.6168342147, 27.3957441269)),
      new CountryBoundingBox("MW", 1, SPATIAL_CONTEXT.makeRectangle(32.6881653175, 35.7719047381, -16.8012997372, -9.23059905359)),
      new CountryBoundingBox("MY", 1, SPATIAL_CONTEXT.makeRectangle(100.085756871, 119.181903925, 0.773131415201, 6.92805288332)),
      new CountryBoundingBox("NA", 1, SPATIAL_CONTEXT.makeRectangle(11.7341988461, 25.0844433937, -29.045461928, -16.9413428687)),
      new CountryBoundingBox("NC", 1, SPATIAL_CONTEXT.makeRectangle(164.029605748, 167.120011428, -22.3999760881, -20.1056458473)),
      new CountryBoundingBox("NE", 1, SPATIAL_CONTEXT.makeRectangle(0.295646396495, 15.9032466977, 11.6601671412, 23.4716684026)),
      new CountryBoundingBox("NG", 1, SPATIAL_CONTEXT.makeRectangle(2.69170169436, 14.5771777686, 4.24059418377, 13.8659239771)),
      new CountryBoundingBox("NI", 1, SPATIAL_CONTEXT.makeRectangle(-87.6684934151, -83.147219001, 10.7268390975, 15.0162671981)),
      new CountryBoundingBox("NL", 1, SPATIAL_CONTEXT.makeRectangle(3.31497114423, 7.09205325687, 50.803721015, 53.5104033474)),
      new CountryBoundingBox("NO", 1, SPATIAL_CONTEXT.makeRectangle(4.99207807783, 31.29341841, 58.0788841824, 80.6571442736)),
      new CountryBoundingBox("NP", 1, SPATIAL_CONTEXT.makeRectangle(80.0884245137, 88.1748043151, 26.3978980576, 30.4227169866)),
      new CountryBoundingBox("NZ", 1, SPATIAL_CONTEXT.makeRectangle(166.509144322, 178.517093541, -46.641235447, -34.4506617165)),
      new CountryBoundingBox("OM", 1, SPATIAL_CONTEXT.makeRectangle(52.0000098, 59.8080603372, 16.6510511337, 26.3959343531)),
      new CountryBoundingBox("PK", 1, SPATIAL_CONTEXT.makeRectangle(60.8742484882, 77.8374507995, 23.6919650335, 37.1330309108)),
      new CountryBoundingBox("PA", 1, SPATIAL_CONTEXT.makeRectangle(-82.9657830472, -77.2425664944, 7.2205414901, 9.61161001224)),
      new CountryBoundingBox("PE", 1, SPATIAL_CONTEXT.makeRectangle(-81.4109425524, -68.6650797187, -18.3479753557, -0.0572054988649)),
      new CountryBoundingBox("PH", 1, SPATIAL_CONTEXT.makeRectangle(117.17427453, 126.537423944, 5.58100332277, 18.5052273625)),
      new CountryBoundingBox("PG", 1, SPATIAL_CONTEXT.makeRectangle(141.000210403, 156.019965448, -10.6524760881, -2.50000212973)),
      new CountryBoundingBox("PL", 1, SPATIAL_CONTEXT.makeRectangle(14.0745211117, 24.0299857927, 49.0273953314, 54.8515359564)),
      new CountryBoundingBox("PR", 1, SPATIAL_CONTEXT.makeRectangle(-67.2424275377, -65.5910037909, 17.946553453, 18.5206011011)),
      new CountryBoundingBox("KP", 1, SPATIAL_CONTEXT.makeRectangle(124.265624628, 130.780007359, 37.669070543, 42.9853868678)),
      new CountryBoundingBox("PT", 1, SPATIAL_CONTEXT.makeRectangle(-9.52657060387, -6.3890876937, 36.838268541, 42.280468655)),
      new CountryBoundingBox("PY", 1, SPATIAL_CONTEXT.makeRectangle(-62.6850571357, -54.2929595608, -27.5484990374, -19.3427466773)),
      new CountryBoundingBox("QA", 1, SPATIAL_CONTEXT.makeRectangle(50.7439107603, 51.6067004738, 24.5563308782, 26.1145820175)),
      new CountryBoundingBox("RO", 1, SPATIAL_CONTEXT.makeRectangle(20.2201924985, 29.62654341, 43.6884447292, 48.2208812526)),
      new CountryBoundingBox("RU", 1, SPATIAL_CONTEXT.makeRectangle(-180.0, 180.0, 41.151416124, 81.2504)),
      new CountryBoundingBox("RW", 1, SPATIAL_CONTEXT.makeRectangle(29.0249263852, 30.8161348813, -2.91785776125, -1.13465911215)),
      new CountryBoundingBox("SA", 1, SPATIAL_CONTEXT.makeRectangle(34.6323360532, 55.6666593769, 16.3478913436, 32.161008816)),
      new CountryBoundingBox("SD", 1, SPATIAL_CONTEXT.makeRectangle(21.93681, 38.4100899595, 8.61972971293, 22.0)),
      new CountryBoundingBox("SS", 1, SPATIAL_CONTEXT.makeRectangle(23.8869795809, 35.2980071182, 3.50917, 12.2480077571)),
      new CountryBoundingBox("SN", 1, SPATIAL_CONTEXT.makeRectangle(-17.6250426905, -11.4678991358, 12.332089952, 16.5982636581)),
      new CountryBoundingBox("SB", 1, SPATIAL_CONTEXT.makeRectangle(156.491357864, 162.398645868, -10.8263672828, -6.59933847415)),
      new CountryBoundingBox("SL", 1, SPATIAL_CONTEXT.makeRectangle(-13.2465502588, -10.2300935531, 6.78591685631, 10.0469839543)),
      new CountryBoundingBox("SV", 1, SPATIAL_CONTEXT.makeRectangle(-90.0955545723, -87.7235029772, 13.1490168319, 14.4241327987)),
      new CountryBoundingBox("SO", 1, SPATIAL_CONTEXT.makeRectangle(40.98105, 51.13387, -1.68325, 12.02464)),
      new CountryBoundingBox("RS", 1, SPATIAL_CONTEXT.makeRectangle(18.82982, 22.9860185076, 42.2452243971, 46.1717298447)),
      new CountryBoundingBox("SR", 1, SPATIAL_CONTEXT.makeRectangle(-58.0446943834, -53.9580446031, 1.81766714112, 6.0252914494)),
      new CountryBoundingBox("SK", 1, SPATIAL_CONTEXT.makeRectangle(16.8799829444, 22.5581376482, 47.7584288601, 49.5715740017)),
      new CountryBoundingBox("SI", 1, SPATIAL_CONTEXT.makeRectangle(13.6981099789, 16.5648083839, 45.4523163926, 46.8523859727)),
      new CountryBoundingBox("SE", 1, SPATIAL_CONTEXT.makeRectangle(11.0273686052, 23.9033785336, 55.3617373725, 69.1062472602)),
      new CountryBoundingBox("SZ", 1, SPATIAL_CONTEXT.makeRectangle(30.6766085141, 32.0716654803, -27.2858794085, -25.660190525)),
      new CountryBoundingBox("SY", 1, SPATIAL_CONTEXT.makeRectangle(35.7007979673, 42.3495910988, 32.312937527, 37.2298725449)),
      new CountryBoundingBox("TD", 1, SPATIAL_CONTEXT.makeRectangle(13.5403935076, 23.88689, 7.42192454674, 23.40972)),
      new CountryBoundingBox("TG", 1, SPATIAL_CONTEXT.makeRectangle(-0.0497847151599, 1.86524051271, 5.92883738853, 11.0186817489)),
      new CountryBoundingBox("TH", 1, SPATIAL_CONTEXT.makeRectangle(97.3758964376, 105.589038527, 5.69138418215, 20.4178496363)),
      new CountryBoundingBox("TJ", 1, SPATIAL_CONTEXT.makeRectangle(67.4422196796, 74.9800024759, 36.7381712916, 40.9602133245)),
      new CountryBoundingBox("TM", 1, SPATIAL_CONTEXT.makeRectangle(52.5024597512, 66.5461503437, 35.2706639674, 42.7515510117)),
      new CountryBoundingBox("TL", 1, SPATIAL_CONTEXT.makeRectangle(124.968682489, 127.335928176, -9.39317310958, -8.27334482181)),
      new CountryBoundingBox("TT", 1, SPATIAL_CONTEXT.makeRectangle(-61.95, -60.895, 10.0, 10.89)),
      new CountryBoundingBox("TN", 1, SPATIAL_CONTEXT.makeRectangle(7.52448164229, 11.4887874691, 30.3075560572, 37.3499944118)),
      new CountryBoundingBox("TR", 1, SPATIAL_CONTEXT.makeRectangle(26.0433512713, 44.7939896991, 35.8215347357, 42.1414848903)),
      new CountryBoundingBox("TW", 1, SPATIAL_CONTEXT.makeRectangle(120.106188593, 121.951243931, 21.9705713974, 25.2954588893)),
      new CountryBoundingBox("TZ", 1, SPATIAL_CONTEXT.makeRectangle(29.3399975929, 40.31659, -11.7209380022, -0.95)),
      new CountryBoundingBox("UG", 1, SPATIAL_CONTEXT.makeRectangle(29.5794661801, 35.03599, -1.44332244223, 4.24988494736)),
      new CountryBoundingBox("UA", 1, SPATIAL_CONTEXT.makeRectangle(22.0856083513, 40.0807890155, 44.3614785833, 52.3350745713)),
      new CountryBoundingBox("UY", 1, SPATIAL_CONTEXT.makeRectangle(-58.4270741441, -53.209588996, -34.9526465797, -30.1096863746)),
      new CountryBoundingBox("US", 35, SPATIAL_CONTEXT.makeRectangle(-171.791110603, -66.96466, 18.91619, 71.3577635769)),
      new CountryBoundingBox("UZ", 1, SPATIAL_CONTEXT.makeRectangle(55.9289172707, 73.055417108, 37.1449940049, 45.5868043076)),
      new CountryBoundingBox("VE", 1, SPATIAL_CONTEXT.makeRectangle(-73.3049515449, -59.7582848782, 0.724452215982, 12.1623070337)),
      new CountryBoundingBox("VN", 1, SPATIAL_CONTEXT.makeRectangle(102.170435826, 109.33526981, 8.59975962975, 23.3520633001)),
      new CountryBoundingBox("VU", 1, SPATIAL_CONTEXT.makeRectangle(166.629136998, 167.844876744, -16.5978496233, -14.6264970842)),
      new CountryBoundingBox("PS", 1, SPATIAL_CONTEXT.makeRectangle(34.9274084816, 35.5456653175, 31.3534353704, 32.5325106878)),
      new CountryBoundingBox("YE", 1, SPATIAL_CONTEXT.makeRectangle(42.6048726743, 53.1085726255, 12.5859504257, 19.0000033635)),
      new CountryBoundingBox("ZA", 1, SPATIAL_CONTEXT.makeRectangle(16.3449768409, 32.830120477, -34.8191663551, -22.0913127581)),
      new CountryBoundingBox("ZM", 1, SPATIAL_CONTEXT.makeRectangle(21.887842645, 33.4856876971, -17.9612289364, -8.23825652429)),
      new CountryBoundingBox("ZW", 1, SPATIAL_CONTEXT.makeRectangle(25.2642257016, 32.8498608742, -22.2716118303, -15.5077869605))
  );

  private static final List<Resource> GET_RESOURCES = ImmutableList.of(
      new Resource("/login", "LoginController", "login", 10),
      new Resource("/users", "UsersController", "index", 2),
      new Resource("/posts", "PostsController", "index", 40),
      new Resource("/posts/45326", "PostsController", "show", 12),
      new Resource("/posts/45326/edit", "PostsController", "edit", 1));

  private static final Map<String, Resource> RESOURCE_MAP = Maps
      .uniqueIndex(GET_RESOURCES, Resource::getResource);

  private static final List<UserId> USER_IDS = ImmutableList.of(
      new UserId(9001, 10),
      new UserId(54351, 1),
      new UserId(74422, 5),
      new UserId(6476752, 12),
      new UserId(6469981, 40));

  private final String source;

  public FakeHttpRawMessageGenerator(String source) {
    this.source = requireNonNull(source);
  }

  public static int rateDeviation(int val, int maxDeviation, Random rand) {
    final int deviationPercent = rand.nextInt(maxDeviation);
    final double x = val / 100.0 * deviationPercent;

    // Add or substract?
    final double result;
    if (rand.nextBoolean()) {
      result = val - x;
    } else {
      result = val + x;
    }

    if (result < 0) {
      return 1;
    } else {
      return Ints.saturatedCast(Math.round(result));
    }
  }

  public static Message generateMessage(GeneratorState state) {
    Message msg = null;
    switch (state.method) {
      case GET:
        msg = simulateGET(state, RANDOM);
        break;
      case POST:
        msg = simulatePOST(state, RANDOM);
        break;
      case DELETE:
        msg = simulateDELETE(state, RANDOM);
        break;
      case PUT:
        msg = simulatePUT(state, RANDOM);
        break;
    }
    return msg;
  }

  private static String shortMessage(DateTime ingestTime, GeneratorState.Method method,
      String resource, int code, int tookMs) {
    return ingestTime + " " + method + " " + resource + " [" + code + "]" + " " + tookMs + "ms";
  }

  private static Map<String, Object> ingestTimeFields(DateTime ingestTime) {
    return ImmutableMap.<String, Object>builder()
        .put("ingest_time", ingestTime.toString())
        .put("ingest_time_epoch", ingestTime.getMillis())
        .put("ingest_time_second", ingestTime.getSecondOfMinute())
        .put("ingest_time_minute", ingestTime.getMinuteOfHour())
        .put("ingest_time_hour", ingestTime.getHourOfDay())
        .put("ingest_time_day", ingestTime.getDayOfMonth())
        .put("ingest_time_month", ingestTime.getMonthOfYear())
        .put("ingest_time_year", ingestTime.getYear())
        .build();
  }

  private static Map<String, Object> resourceFields(Resource resource) {
    return ImmutableMap.<String, Object>builder()
        .put("resource", resource.getResource())
        .put("controller", resource.getController())
        .put("action", resource.getAction())
        .build();
  }

  private static Message createMessage(GeneratorState state, int httpCode, Resource resource,
      int tookMs, DateTime ingestTime) {
    final Message msg = new Message(
        shortMessage(ingestTime, state.method, state.resource, httpCode, tookMs), state.source,
        Tools.nowUTC());
    msg.addFields(ingestTimeFields(ingestTime));
    msg.addFields(resourceFields(resource));
    msg.addField("ticks", System.nanoTime());
    msg.addField("http_method", state.method.name());
    msg.addField("http_response_code", httpCode);
    msg.addField("user_id", state.userId);
    msg.addField("took_ms", tookMs);

    // lat,lon!
    msg.addField("location", state.geoY + ", " + state.geoX);

    return msg;
  }

  public static Message simulateGET(GeneratorState state, Random rand) {
    int msBase = 50;
    int deviation = 30;
    int code = state.isSuccessful ? 200 : 500;
    if (!state.isSuccessful && state.isTimeout) {
      // Simulate an internal API timeout from time to time.
      msBase = 5000;
      deviation = 10;
      code = 504;
    } else if (rand.nextInt(500) == 1) {
      // ...or just something a bit too slow
      msBase = 400;
    }

    final DateTime ingestTime = Tools.nowUTC();
    final Resource resource = RESOURCE_MAP.get(state.resource);
    final int tookMs = rateDeviation(msBase, deviation, rand);

    return createMessage(state, code, resource, tookMs, ingestTime);
  }

  private static Message simulatePOST(GeneratorState state, Random rand) {
    int msBase = 150;
    int deviation = 20;
    int code = state.isSuccessful ? 201 : 500;
    if (!state.isSuccessful && state.isTimeout) {
      // Simulate an internal API timeout from time to time.
      msBase = 5000;
      deviation = 18;
      code = 504;
    } else if (rand.nextInt(500) == 1) {
      // ...or just something a bit too slow
      msBase = 400;
    }

    final DateTime ingestTime = Tools.nowUTC();
    final Resource resource = RESOURCE_MAP.get(state.resource);
    final int tookMs = rateDeviation(msBase, deviation, rand);

    return createMessage(state, code, resource, tookMs, ingestTime);
  }

  private static Message simulatePUT(GeneratorState state, Random rand) {
    int msBase = 100;
    int deviation = 30;
    int code = state.isSuccessful ? 200 : 500;
    if (!state.isSuccessful && state.isTimeout) {
      // Simulate an internal API timeout from time to time.
      msBase = 5000;
      deviation = 18;
      code = 504;
    } else if (rand.nextInt(500) == 1) {
      // ...or just something a bit too slow
      msBase = 400;
    }

    final DateTime ingestTime = Tools.nowUTC();
    final Resource resource = RESOURCE_MAP.get(state.resource);
    final int tookMs = rateDeviation(msBase, deviation, rand);

    return createMessage(state, code, resource, tookMs, ingestTime);
  }

  private static Message simulateDELETE(GeneratorState state, Random rand) {
    int msBase = 75;
    int deviation = 40;
    int code = state.isSuccessful ? 204 : 500;
    if (!state.isSuccessful && state.isTimeout) {
      // Simulate an internal API timeout from time to time.
      msBase = 5000;
      deviation = 18;
      code = 504;
    } else if (rand.nextInt(500) == 1) {
      // ...or just something a bit too slow
      msBase = 400;
    }

    final DateTime ingestTime = Tools.nowUTC();
    final Resource resource = RESOURCE_MAP.get(state.resource);
    final int tookMs = rateDeviation(msBase, deviation, rand);

    return createMessage(state, code, resource, tookMs, ingestTime);
  }

  public GeneratorState generateState() {
    final GeneratorState generatorState = new GeneratorState();

    final int methodProb = RANDOM.nextInt(100);
    final int successProb = RANDOM.nextInt(100);

    generatorState.source = source;
    generatorState.isSuccessful = successProb < 98;
    generatorState.isTimeout = RANDOM.nextInt(5) == 1;
    generatorState.isSlowRequest = RANDOM.nextInt(500) == 1;
    generatorState.userId = ((UserId) getWeighted(USER_IDS)).getId();
    generatorState.resource = ((Resource) getWeighted(GET_RESOURCES)).getResource();

    if (methodProb <= 85) {
      generatorState.method = GET;
    } else if (methodProb > 85 && methodProb <= 90) {
      generatorState.method = POST;
    } else if (methodProb > 90 && methodProb <= 95) {
      generatorState.method = DELETE;
    } else {
      generatorState.method = PUT;
    }

    final Point location = ((CountryBoundingBox) getWeighted(BOUNDING_BOXES)).randomLocation();
    generatorState.geoX = (double)Math.round(location.getX() * 10d) / 10d;
    generatorState.geoY = (double)Math.round(location.getY() * 10d) / 10d;

    return generatorState;
  }

  private Weighted getWeighted(List<? extends Weighted> list) {
    while (true) {
      int x = RANDOM.nextInt(MAX_WEIGHT);
      Weighted obj = list.get(RANDOM.nextInt(list.size()));

      if (obj.getWeight() >= x) {
        return obj;
      }
    }
  }

  private static abstract class Weighted {

    protected final int weight;

    protected Weighted(int weight) {
      if (weight <= 0 || weight > MAX_WEIGHT) {
        throw new RuntimeException("Invalid resource weight: " + weight);
      }

      this.weight = weight;
    }

    public int getWeight() {
      return weight;
    }

  }

  private static class Resource extends Weighted {

    private final String resource;
    private final String controller;
    private final String action;

    public Resource(String resource, String controller, String action, int weight) {
      super(weight);

      this.resource = resource;
      this.controller = controller;
      this.action = action;
    }

    public String getResource() {
      return resource;
    }

    public String getController() {
      return controller;
    }

    public String getAction() {
      return action;
    }
  }

  private static class UserId extends Weighted {

    private final int id;

    public UserId(int id, int weight) {
      super(weight);

      this.id = id;
    }

    public int getId() {
      return id;
    }
  }

  public static class GeneratorState {

    public String source;
    public boolean isSuccessful;
    public Method method;
    public boolean isTimeout;
    public boolean isSlowRequest;
    public int userId;
    public String resource;
    public double geoX;
    public double geoY;

    public enum Method {
      GET, POST, DELETE, PUT
    }
  }

  private static class CountryBoundingBox extends Weighted {
    private final String code;
    private final Rectangle bbox;

    public CountryBoundingBox(String code, int weight, Rectangle bbox) {
      super(weight);
      this.code = code;
      this.bbox = bbox;
    }

    public Point randomLocation() {
      final double xOffset = bbox.getWidth() * RANDOM.nextDouble();
      final double yOffset = bbox.getHeight() * RANDOM.nextDouble();

      return SPATIAL_CONTEXT.makePoint(bbox.getMinX() + xOffset, bbox.getMinY() + yOffset);
    }

    public String getCode() {
      return code;
    }
  }
}
