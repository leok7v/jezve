package org.jezve.svg;

import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.io.*;

import org.jezve.svg.batik.*;
import org.jezve.svg.batik.MultipleGradientPaint; // java 1.6 disambiguashion
import org.jezve.svg.batik.RadialGradientPaint; // java 1.6 disambiguashion
import org.jezve.svg.batik.LinearGradientPaint; // java 1.6 disambiguashion

public class SVG {

    static {
        if (System.getProperty("os.name").toLowerCase().indexOf("mac os x") >= 0) {
            assert "false".equals(System.getProperty("apple.awt.graphics.UseQuartz"));
            // On Java < 1.6
            // openclipart-0.18-svgonly/clipart/office/magnifying_glass_01.svg
            // calls getRaster() with user rather than device coordinates ~11,000x11,000
            // see: http://lists.apple.com/archives/java-dev/2005/Nov/msg00222.html
            // http://lists.apple.com/archives/Java-dev/2007/Jun/msg00067.html
        }
    }

    private Root root;
    private final HashMap map = new HashMap(); // id(String) -> Element
    private final Map css = new HashMap(); // classname(String) -> parsedStyle(Map)
    private final HashMap loadedFonts = new HashMap();
    private final static InputSource DUMMY = new InputSource(new ByteArrayInputStream(new byte[0]));
    private static BufferedImage bi1x1;

    private SVG() {
    }


    public static SVG read(InputStream is) throws IOException {
        SVG svg = new SVG();
        svg.load(new InputSource(svg.createDocumentInputStream(is)));
        return svg.getRoot() == null ? null : svg;
    }

    private static BufferedImage get1x1() {
        if (bi1x1 == null) {
            bi1x1 = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = bi1x1.createGraphics();
            g.setColor(new Color(0, 0, 0, 0)); // transparent
            g.fill(new Rectangle(0, 0, 1, 1));
            g.dispose();
        }
        return bi1x1;
    }

    private InputStream createDocumentInputStream(InputStream is) throws IOException {
        BufferedInputStream bin = new BufferedInputStream(is);
        bin.mark(2);
        int b0 = bin.read();
        int b1 = bin.read();
        bin.reset();
        // Check for gzip magic number
        if ((b1 << 8 | b0) == GZIPInputStream.GZIP_MAGIC) {
            return new GZIPInputStream(bin);
        } else {
            return bin;
        }
    }

    private void load(InputSource is) throws IOException {
        Loader loader = new Loader();
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);
        try {
            XMLReader reader = XMLReaderFactory.createXMLReader();
            reader.setEntityResolver(new EntityResolver() {
                public InputSource resolveEntity(String publicId, String systemId) {
                    return DUMMY; // to prevent going out to network reading DTD
                }
            });
            reader.setContentHandler(loader);
            reader.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
            reader.setFeature("http://xml.org/sax/features/namespaces", false);
            reader.parse(is);
        } catch (SAXParseException e) {
            throw new IOException(e.getMessage());
        } catch (SAXException e) {
            throw new IOException(e.getMessage());
        }
    }

    public void render(Graphics2D g) {
        root.render(g);
    }

    private void registerFont(Font font) {
        loadedFonts.put(font.getFontFace().getFontFamily(), font);
    }

    private void registerStyle(String classNames, Map map) {
        css.put(classNames, map);
    }

    Map getStyle(String className) {
        return (Map)css.get(className);
    }

    Font getFont(String fontName) {
        return (Font)loadedFonts.get(fontName);
    }

    Element get(String name) {
        return (Element)map.get(name);
    }

    void put(String name, Element node) {
        map.put(name, node);
    }

    void removeElement(String name) {
        map.remove(name);
    }

    public Root getRoot() {
        return root;
    }

    void setRoot(Root r) {
        root = r;
    }

    private class Loader extends DefaultHandler {

        private final HashMap nodeClasses = new HashMap();
        private final LinkedList buildStack = new LinkedList();
        private final HashSet ignoreClasses = new HashSet();

        private Loader() {
            nodeClasses.put("a", A.class);
            nodeClasses.put("circle", Circle.class);
            nodeClasses.put("clippath", ClipPath.class);
            nodeClasses.put("defs", Defs.class);
            nodeClasses.put("desc", Desc.class);
            nodeClasses.put("ellipse", Ellipse.class);
            nodeClasses.put("filter", Filter.class);
            nodeClasses.put("font", Font.class);
            nodeClasses.put("font-face", FontFace.class);
            nodeClasses.put("g", Group.class);
            nodeClasses.put("glyph", Glyph.class);
            nodeClasses.put("image", Image.class);
            nodeClasses.put("line", Line.class);
            nodeClasses.put("lineargradient", LinearGradient.class);
            nodeClasses.put("metadata", Metadata.class);
            nodeClasses.put("missing-glyph", MissingGlyph.class);
            nodeClasses.put("path", Path.class);
            nodeClasses.put("pattern", Pattern.class);
            nodeClasses.put("polygon", Polygon.class);
            nodeClasses.put("polyline", Polyline.class);
            nodeClasses.put("radialgradient", RadialGradient.class);
            nodeClasses.put("rect", Rect.class);
            nodeClasses.put("shape", ShapeElement.class);
            nodeClasses.put("stop", Stop.class);
            nodeClasses.put("style", Style.class);
            nodeClasses.put("svg", Root.class);
            nodeClasses.put("symbol", Symbol.class);
            nodeClasses.put("text", Text.class);
            nodeClasses.put("title", Title.class);
            nodeClasses.put("tspan", Text.class);
            nodeClasses.put("use", Use.class);

            ignoreClasses.add("midpointstop");
            ignoreClasses.add("namedview");
            ignoreClasses.add("fegaussianblur");
            ignoreClasses.add("feblend");
            ignoreClasses.add("fecolormatrix");
        }

        public void fatalError(SAXParseException e) throws SAXException {
            throw e;
        }

        public void startDocument() throws SAXException {
        }

        public void endDocument() throws SAXException {
        }

        public void startElement(String namespaceURI, String name, String qualified, Attributes attrs) throws SAXException {
            name = qualified.toLowerCase();
            int ix = name.indexOf(':');
            if (ix > 0 && ix < name.length() - 1) {
                name = name.substring(ix + 1);
            }
            Object obj = nodeClasses.get(name);
            if (obj == null) {
                Metadata md = null;
                for (Iterator i = buildStack.iterator(); i.hasNext();) {
                    Object o = i.next();
                    if (o instanceof Metadata) {
                        md = (Metadata)o;
                        break;
                    }
                }
                if (!ignoreClasses.contains(name) && md == null) {
                    // silently ignore all tags inside metadata
//                  System.err.println("SVG.Loader: Could not identify tag '" + name + "'");
                }
                return;
            }
            try {
                Class cls = (Class)obj;
                Element e = (Element)cls.newInstance();
                Element parent = null;
                if (buildStack.size() != 0) {
                    parent = (Element)buildStack.getLast();
                }
                e.loaderStartElement(SVG.this, attrs, parent);
                buildStack.addLast(e);
            } catch (IllegalAccessException e) {
                throw new Error(e);
            } catch (InstantiationException e) {
                throw new Error(e);
            }
        }

        public void endElement(String namespaceURI, String name, String qualified) throws SAXException {
            name = qualified.toLowerCase();
            int ix = name.indexOf(':');
            if (ix > 0 && ix < name.length() - 1) {
                name = name.substring(ix + 1);
            }
            Object obj = nodeClasses.get(name);
            if (obj == null) {
                return;
            }
            Element e = (Element)buildStack.removeLast();
            e.loaderEndElement();
            Element parent = null;
            if (buildStack.size() != 0) {
                parent = (Element)buildStack.getLast();
            }
            if (parent != null) {
                parent.loaderAddChild(e);
            } else {
                setRoot((Root)e);
                build(e);
                resolve(e);
            }
        }

        private void build(Element e) {
            if (e != null) {
                for (Iterator i = e.getChildren().iterator(); i.hasNext();) {
                    Element c = (Element)i.next();
                    build(c);
                }
                e.build();
            }
        }

        private void resolve(Element e) {
            if (e != null) {
                e.resolve();
                e.clear();
                for (Iterator i = e.getChildren().iterator(); i.hasNext();) {
                    Element c = (Element)i.next();
                    resolve(c);
                }
            }

        }

        public void characters(char buf[], int offset, int len) throws SAXException {
            if (buildStack.size() != 0) {
                Element parent = (Element)buildStack.getLast();
                String s = new String(buf, offset, len);
                parent.loaderAddText(s);
            }
        }

        public void processingInstruction(String target, String data) throws SAXException {
        }

    }

    static abstract class FillElement extends Element {

        abstract Paint getPaint(Rectangle2D bounds, AffineTransform xform);

    }

    static class TransformableElement extends Element {

        private AffineTransform xform;

        AffineTransform getTransform() {
            return xform;
        }

        protected void build() {
            super.build();
            String t = getString("transform");
            xform = t != null ? parseTransform(t) : null;
        }

        protected final Shape shapeToParent(Shape shape) {
            return xform == null ? shape : xform.createTransformedShape(shape);
        }

        protected final Rectangle2D boundsToParent(Rectangle2D rect) {
            return xform == null ? rect : xform.createTransformedShape(rect).getBounds2D();
        }
    }

    static abstract class RenderableElement extends TransformableElement {

        private AffineTransform savedXform;
        private Shape savedClip;
        static final int VECTOR_EFFECT_NONE = 0;
        static final int VECTOR_EFFECT_NON_SCALING_STROKE = 1;
        private int vectorEffect;
        private Shape clipPath;
        private int clipPathUnits = ClipPath.CP_USER_SPACE_ON_USE;
        private String href;

        protected void build() {
            super.build();
            String ve = getString("vector-effect");
            if (ve != null) {
                if ("non-scaling-stroke".equalsIgnoreCase(ve)) {
                    vectorEffect = VECTOR_EFFECT_NON_SCALING_STROKE;
                } else {
                    vectorEffect = VECTOR_EFFECT_NONE;
                }
            } else {
                vectorEffect = VECTOR_EFFECT_NONE;
            }
            href = parseHref(getStyleString("clip-path"));
        }

        protected void resolve() {
            super.resolve();
            if (href != null) {
                ClipPath e = (ClipPath)getRoot().get(href);
                if (e != null) {
                    e.resolve();
                    clipPath = e.getClipPathShape();
                    clipPathUnits = e.getClipPathUnits();
                    href = null;
                }
            }
        }

        abstract void render(Graphics2D g);

        abstract Rectangle2D getBoundingBox();

        protected void beginLayer(Graphics2D g) {
            savedXform = g.getTransform();
            savedClip = g.getClip();
            if (getTransform() != null) {
                g.transform(getTransform());
            }
            Shape cp = clipPath;
            if (cp != null) {
                if (clipPathUnits == ClipPath.CP_OBJECT_BOUNDING_BOX && (this instanceof ShapeElement)) {
                    Rectangle2D rect = this.getBoundingBox();
                    AffineTransform at = AffineTransform.getScaleInstance(rect.getWidth(), rect.getHeight());
                    cp = at.createTransformedShape(cp);
                }
/*
                // The intersect() is unberably slow for Oxygen esd.svg[z] (2.2MB uncompressed)
                if (savedClip != null) {
                    Area newClip = new Area(savedClip);
                    newClip.intersect(cp instanceof Area ? (Area)cp : new Area(cp));
                    g.setClip(newClip);
                } else {
                    g.setClip(cp);
                }
*/
                g.setClip(cp); // this is not accurate but much faster
            }
        }

        protected void finishLayer(Graphics2D g) {
            g.setTransform(savedXform);
            g.setClip(savedClip);
        }

        int getVectorEffect() {
            return vectorEffect;
        }

    }

    static abstract class ShapeElement extends RenderableElement {

        private float strokeWidthScalar = 1f;
        private boolean isVisible;
        private Paint fillPaint;
        private FillElement fillElement;
        private String hrefFillElement;
        private float opacity;
        private float fillOpacity;
        private Paint strokePaint;
        private String hrefStrokeFill;
        private FillElement strokeFill;
        private float[] strokeDashArray;
        private float strokeDashOffset;
        private int strokeLinecap = BasicStroke.CAP_BUTT;
        private int strokeLinejoin = BasicStroke.JOIN_MITER;
        private float strokeMiterLimit = 4;
        private float strokeOpacity = 1;
        private float strokeWidth = 1;
        private boolean hasStroke;

        abstract void render(java.awt.Graphics2D g);

        protected void setStrokeWidthScalar(float strokeWidthScalar) {
            this.strokeWidthScalar = strokeWidthScalar;
        }

        float getStrokeWidthScalar() {
            return strokeWidthScalar;
        }

        protected boolean isVisible() {
            return isVisible;
        }

        protected void build() {
            super.build();
            String v = getString("visibility");
            isVisible = v == null || "visible".equalsIgnoreCase(v);
            if ("none".equalsIgnoreCase(getString("display"))) {
                isVisible = false;
            }
            if (!isVisible) {
                return;
            }
            String f = getStyleString("fill");
            if (f != null) {
                if ("none".equalsIgnoreCase(f)) {
                    fillPaint = null;
                } else {
                    if (f.toLowerCase().startsWith("url(") && f.endsWith(")")) {
                        hrefFillElement = parseHref(f);
                    } else {
                        String s = getStyleString("fill");
                        if (s != null && s.length() > 1 &&
                                Parser.isDigit(s.charAt(0)) && s.indexOf('.') > 0) {
                            // papapishu/papapishu_Tools.svg
                            // inkscape:version="0.46dev+devel"
                            //  <path style="opacity:0.42458101;fill:0.42458101;
                            fillPaint = Color.BLACK;
                        } else {
                            fillPaint = getStyleColor("fill", Color.BLACK);
                        }
                    }
                }
            } else {
                fillPaint = Color.BLACK;
            }
            opacity = getStyleRatioValue("opacity", 1);
            fillOpacity = getStyleRatioValue("fill-opacity", 1);
            String s = getStyleString("stroke");
            if (s != null) {
                hasStroke = true;
                if ("none".equalsIgnoreCase(s)) {
                    strokePaint = null;
                } else {
                    if (s.toLowerCase().startsWith("url(") && s.endsWith(")")) {
                        hrefStrokeFill = parseHref(s);
                    } else {
                        strokePaint = getStyleColor("stroke", null);
                    }
                }
            }
            String sd = getStyleString("stroke-dasharray");
            if (sd != null) {
                if ("none".equalsIgnoreCase(sd)) {
                    strokeDashArray = null;
                } else {
                    strokeDashArray = getStyleFloats("stroke-dasharray");
                    boolean allZero = true;
                    for (int i = 0; i < strokeDashArray.length; i++) {
                        if (strokeDashArray[i] != 0) {
                            allZero = false;
                            break;
                        }
                    }
                    if (allZero) {
                        strokeDashArray = null;
                    }
                }
            }
            strokeDashOffset = getStyleFloatUnits("stroke-dashoffset", 0);
            String slc = getStyleString("stroke-linecap");
            if (slc != null) {
                if ("round".equalsIgnoreCase(slc)) {
                    strokeLinecap = BasicStroke.CAP_ROUND;
                } else if ("square".equalsIgnoreCase(slc)) {
                    strokeLinecap = BasicStroke.CAP_SQUARE;
                }
            }
            String slj = getStyleString("stroke-linejoin");
            if (slj != null) {
                if ("round".equalsIgnoreCase(slj)) {
                    strokeLinejoin = BasicStroke.JOIN_ROUND;
                } else if ("bevel".equalsIgnoreCase(slj)) {
                    strokeLinejoin = BasicStroke.JOIN_BEVEL;
                }
            }
            strokeMiterLimit = Math.max(getStyleFloatUnits("stroke-miterlimit", 0), 1);
            strokeOpacity = getStyleRatioValue("stroke-opacity", 1);
            strokeWidth = getStyleFloatUnits("stroke-width", 1);
        }

        protected void resolve() {
            super.resolve();
            if (hrefFillElement != null) {
                Element e = getRoot().get(hrefFillElement);
                if (e != null) { // kde.svgz XMLID_29_
                    fillElement = (FillElement)e;
                    fillElement.resolve();
                    hrefFillElement = null;
                }
            }
            if (hrefStrokeFill != null) {
                Element e = getRoot().get(hrefStrokeFill);
                if (e != null) {
                    strokeFill = (FillElement)e;
                    strokeFill.resolve();
                    hrefStrokeFill = null;
                }
            }
        }

        protected void renderShape(Graphics2D g, Shape shape) {
            if (!isVisible) {
                return;
            }
            Paint paintFill = fillPaint;
            if (paintFill == null) {
                if (fillElement != null) {
                    Rectangle2D bounds = shape.getBounds2D();
                    AffineTransform xform = g.getTransform();
                    paintFill = fillElement.getPaint(bounds, xform);
                }
            }
            float opacityOfFill   = fillOpacity * opacity;
            if (paintFill != null && opacityOfFill > 0) {
                if (opacityOfFill < 1) {
                    Composite cachedComposite = g.getComposite();
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacityOfFill));
                    g.setPaint(paintFill);
                    g.fill(shape);
                    g.setComposite(cachedComposite);
                } else {
                    g.setPaint(paintFill);
                    if (shape instanceof GeneralPath) {
                        g.fill(shape);
                    } else if (shape instanceof Rectangle) {
                        g.fill(shape);
                    } else  if (shape instanceof Rectangle2D) {
                        g.fill(shape);
                    } else  if (shape instanceof RoundRectangle2D) {
                        g.fill(shape);
                    } else {
                        g.fill(shape);
                    }
                }
            }
            Paint paintStroke = strokePaint;
            if (paintStroke == null) {
                if (strokeFill != null) {
                    Rectangle2D bounds = shape.getBounds2D();
                    AffineTransform xform = g.getTransform();
                    paintStroke = strokeFill.getPaint(bounds, xform);
                }
            }
            float opacityOfStroke = strokeOpacity * opacity;
            if (paintStroke != null && opacityOfStroke > 0) {
                float widthOfStroke = strokeWidth * strokeWidthScalar;
                BasicStroke stroke;
                if (strokeDashArray == null) {
                    stroke = new BasicStroke(widthOfStroke, strokeLinecap, strokeLinejoin, strokeMiterLimit);
                } else {
                    stroke = new BasicStroke(widthOfStroke, strokeLinecap, strokeLinejoin, strokeMiterLimit, strokeDashArray,
                            strokeDashOffset);
                }
                Shape strokeShape = stroke.createStrokedShape(shape);
                if (opacityOfStroke < 1f) {
                    Composite cachedComposite = g.getComposite();
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacityOfStroke));
                    g.setPaint(paintStroke);
                    g.fill(strokeShape);
                    g.setComposite(cachedComposite);
                } else {
                    g.setPaint(paintStroke);
                    g.fill(strokeShape);
                }
            }
        }

        abstract Shape getShape();

        protected final Rectangle2D includeStrokeInBounds(Rectangle2D rect) {
            if (hasStroke) {
                rect.setRect(rect.getX() - strokeWidth / 2, rect.getY() - strokeWidth / 2,
                        rect.getWidth() + strokeWidth,
                        rect.getHeight() + strokeWidth);
            }
            return rect;
        }
    }

    static class Group extends ShapeElement {

        private Rectangle2D boundingBox;
        private Shape shape;

        void render(Graphics2D g) {
            if (!isVisible()) {
                return;
            }
            beginLayer(g);
            for (Iterator i = getChildren().iterator(); i.hasNext();) {
                Element e = (Element)i.next();
                if (e instanceof RenderableElement) {
                    RenderableElement r = (RenderableElement)e;
                    r.render(g);
                }
            }
            finishLayer(g);
        }

        Shape getShape() {
            return shape == null ? calculateShape() : shape;
        }

        private Shape calculateShape() {
            Area a = new Area();
            for (Iterator i = getChildren().iterator(); i.hasNext();) {
                Element e = (Element)i.next();
                if (e instanceof ShapeElement) {
                    ShapeElement se = (ShapeElement)e;
                    Shape shape = se.getShape();
                    if (shape != null) {
                        a.add(new Area(shape));
                    }
                }
            }
            shape = shapeToParent(a);
            return shape;
        }

        public Rectangle2D getBoundingBox() {
            return boundingBox == null ? calculateBoundingBox() : boundingBox;
        }

        private Rectangle2D calculateBoundingBox() {
            Rectangle2D r = null;
            for (Iterator i = getChildren().iterator(); i.hasNext();) {
                Element e = (Element)i.next();
                if (e instanceof RenderableElement) {
                    RenderableElement re = (RenderableElement)e;
                    Rectangle2D bounds = re.getBoundingBox();
                    if (bounds != null) {
                        if (r == null) {
                            r = bounds;
                        } else {
                            r = r.createUnion(bounds);
                        }
                    }
                }
            }
            if (r == null) {
                r = GraphicsUtil.EMPTY_BOX;
            }
            boundingBox = boundsToParent(r);
            return boundingBox;
        }
    }

    static class A extends Group {

        private String href;
        private String title;

        protected void build() {
            super.build();
            href = getStyleHref();
            title = getString("xlink:title");
        }

        String getHref() {
            return href;
        }

        String getTitle() {
            return title;
        }
    }

    static class Metadata extends Element {

    }

    static class Defs extends TransformableElement {

        /* Needs to be present for <defs><style>...</style></defs> to be processed */

    }

    static class Desc extends Element {

        private StringBuffer text = new StringBuffer();

        protected void loaderAddText(String s) {
            text.append(s);
        }

        String getText() {
            return text.toString();
        }
    }

    static class Circle extends ShapeElement {

        private final Ellipse2D.Float circle = new Ellipse2D.Float();

        protected void build() {
            super.build();
            // http://download.openclipart.org/downloads/ daily_SVG_snapshot.tar.bz2 19-May-2008
            // daily_SVG_snapshot/Anonymous/Anonymous_flag_of_Uruguay_2.svg  <circle r="11"/>
            if (getString("cx") != null && getString("cy") != null && getString("r") != null) {
                float cx = getFloatUnits("cx");
                float cy = getFloatUnits("cy");
                float r = getFloatUnits("r");
                circle.setFrame(cx - r, cy - r, r * 2f, r * 2f);
            }
        }

        void render(Graphics2D g) {
            beginLayer(g);
            renderShape(g, circle);
            finishLayer(g);
        }

        Shape getShape() {
            return shapeToParent(circle);
        }

        Rectangle2D getBoundingBox() {
            return boundsToParent(includeStrokeInBounds(circle.getBounds2D()));
        }
    }

    static class Ellipse extends ShapeElement {

        private Ellipse2D.Float ellipse = new Ellipse2D.Float();

        protected void build() {
            super.build();
            // Jesus_Art/Jesus_Art_Jesus_Art_1.svg
            if (getString("cx") != null && getString("cy") != null &&
                    getString("rx") != null && getString("ry") != null) {
                float cx = getFloatUnits("cx");
                float cy = getFloatUnits("cy");
                float rx = getFloatUnits("rx");
                float ry = getFloatUnits("rx");
                ellipse.setFrame(cx - rx, cy - ry, rx * 2f, ry * 2f);
            }
        }

        void render(Graphics2D g) {
            beginLayer(g);
            renderShape(g, ellipse);
            finishLayer(g);
        }

        Shape getShape() {
            return shapeToParent(ellipse);
        }

        Rectangle2D getBoundingBox() {
            return boundsToParent(includeStrokeInBounds(ellipse.getBounds2D()));
        }

    }

    static class Line extends ShapeElement {

        private Line2D.Float line;

        protected void build() {
            super.build();
            float x1 = getFloatUnits("x1");
            float y1 = getFloatUnits("y1");
            float x2 = getFloatUnits("x2");
            float y2 = getFloatUnits("y2");
            line = new Line2D.Float(x1, y1, x2, y2);
        }

        void render(Graphics2D g) {
            beginLayer(g);
            renderShape(g, line);
            finishLayer(g);
        }

        Shape getShape() {
            return shapeToParent(line);
        }

        Rectangle2D getBoundingBox() {
            return boundsToParent(includeStrokeInBounds(line.getBounds2D()));
        }
    }

    static class Title extends Element {

        StringBuffer text = new StringBuffer();

        void loaderAddText(SVG svg, String text) {
            this.text.append(text);
        }

        String getText() {
            return text.toString();
        }
    }

    static class Filter extends Element {

    }

    static class Glyph extends MissingGlyph {

        private String text;

        protected void build() {
            super.build();
            text = getString("unicode");
        }

        String getText() {
            return text;
        }
    }

    static class Style extends Element {

        private String type;
        private final StringBuffer text = new StringBuffer();

        protected void loaderAddText(String s) {
            text.append(s);
        }

        protected void loaderEndElement() {
            super.loaderEndElement();
            type = getString("type");
            String s = text.toString().trim();
            for (;;) {
                int ix = s.indexOf('{');
                if (ix < 0) {
                    break;
                }
                String className = s.substring(0, ix).trim();
                if (className.startsWith(".")) {
                    className = className.substring(1);
                }
                int ix2 = s.indexOf('}', ix);
                if (ix2 < 0) {
                    break;
                }
                String style = s.substring(ix + 1, ix2).trim();
                Map m = new HashMap();
                parseStyle(style, m);
                if (m.size() > 0) {
                    getRoot().registerStyle(className, m);
                }
                s = s.substring(ix2 + 1).trim();
            }
        }

        String getType() {
            return type;
        }
    }

    static class Symbol extends Group {

        private AffineTransform viewXform;
        private Rectangle2D viewBox;

        protected void build() {
            super.build();
            float[] vb = getFloats("viewBox");
            if (vb != null) {
                viewBox = new Rectangle2D.Float(vb[0], vb[1], vb[2], vb[3]);
            }
            if (viewBox == null) {
                viewBox = new Rectangle(0, 0, 1, 1);
            }
            //  TODO: Transform pattern onto unit square... Leo: Why?
            viewXform = new AffineTransform();
            viewXform.scale(1.0 / viewBox.getWidth(), 1.0 / viewBox.getHeight());
            viewXform.translate(-viewBox.getX(), -viewBox.getY());
        }

        void render(Graphics2D g) {
            AffineTransform savedXform = g.getTransform();
            g.transform(viewXform);
            super.render(g);
            g.setTransform(savedXform);
        }

        Shape getShape() {
            Shape shape = super.getShape();
            return viewXform.createTransformedShape(shape);
        }

        public Rectangle2D getBoundingBox() {
            Rectangle2D rect = super.getBoundingBox();
            return viewXform.createTransformedShape(rect).getBounds2D();
        }

    }

    static class Polygon extends ShapeElement {

        private GeneralPath path;

        protected void build() {
            super.build();
            String ps = getString("points");
            String fr = getStyleString("fill-rule", "nonzero");
            int fillRule = "evenodd".equalsIgnoreCase(fr) ?
                    GeneralPath.WIND_EVEN_ODD : GeneralPath.WIND_NON_ZERO;
            // http://download.openclipart.org/downloads/ daily_SVG_snapshot.tar.bz2 19-May-2008
            // daily_SVG_snapshot/Anonymous/Anonymous_Australia.svg  <polygon id="polygon1462" style="fill:#888888;stroke:none" />
            if (ps != null) {
                buildPath(fillRule, ps);
            }
        }

        private void buildPath(int fillRule, String ps) {
            // Generator: Adobe Illustrator 11.0, SVG Export Plug-In . SVG Version: 6.0.0 Build 78)
            // orangeobject/orangeobject_background-ribbon.svg
            // <polygon i:knockout="Off" fill="none" points="1-1 4-1 4-4 1-4 "/>
            float[] points = Element.parseFloats(ps);
            path = new GeneralPath(fillRule, points.length / 2);
            path.moveTo(points[0], points[1]);
            for (int i = 2; i < points.length; i += 2) {
                path.lineTo(points[i], points[i + 1]);
            }
            path.closePath();
        }

        void render(Graphics2D g) {
            if (path != null) {
                beginLayer(g);
                renderShape(g, path);
                finishLayer(g);
            }
        }

        Shape getShape() {
            return path == null ? GraphicsUtil.EMPTY_BOX : shapeToParent(path);
        }

        Rectangle2D getBoundingBox() {
            return path == null ? GraphicsUtil.EMPTY_BOX :
                    boundsToParent(includeStrokeInBounds(path.getBounds2D()));
        }
    }

    static class Polyline extends ShapeElement {

        private GeneralPath path;

        protected void build() {
            super.build();
            String ps = getString("points");
            String fr = getStyleString("fill-rule", "nonzero");
            int fillRule = "evenodd".equalsIgnoreCase(fr) ?
                    GeneralPath.WIND_EVEN_ODD : GeneralPath.WIND_NON_ZERO;
            buildPath(fillRule, ps);
        }

        private void buildPath(int fillRule, String ps) {
            float[] points = Element.parseFloats(ps);
            path = new GeneralPath(fillRule, points.length / 2);
            path.moveTo(points[0], points[1]);
            for (int i = 2; i < points.length; i += 2) {
                path.lineTo(points[i], points[i + 1]);
            }
        }

        void render(Graphics2D g) {
            beginLayer(g);
            renderShape(g, path);
            finishLayer(g);
        }

        Shape getShape() {
            return shapeToParent(path);
        }

        Rectangle2D getBoundingBox() {
            return boundsToParent(includeStrokeInBounds(path.getBounds2D()));
        }

    }

    static class Stop extends Element {

        private float offset = 0f;
        private Color color = Color.BLACK;

        protected void build() {
            super.build();
            Units os = getUnits("offset");
            if (os != null) {
                offset = os.getValue();
                if (os.getKind() == Units.KIND_PERCENT) {
                    offset /= 100f;
                }
                if (offset > 1) {
                    offset = 1;
                }
                if (offset < 0) {
                    offset = 0;
                }
            }
            String s = getStyleString("stop-color");
            if (s == null) {
                // zeimusu/zeimusu_Black_Watch.svg
                color = null;
            } else if (s.length() >= 4 && s.substring(0, 4).equalsIgnoreCase("url(")) {
                // error in Chrisdesign/Chrisdesign_weapon_shield.svg:
                // <stop style="stop-color:url(#linearGradient4192);" id="stop4202"/>
                color = null;
            } else {
                color = getStyleColor("stop-color", Color.BLACK);
            }
            float opacity = getStyleRatioValue("stop-opacity", 1);
            assert 0 <= opacity && opacity <= 1f : "stop-opacity=" + opacity;
            if (opacity < 1f) {
                color = new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.round(255 * opacity));
            }
        }

        protected void resolve() {
            super.resolve();
            if (color == null) {
                color = Color.BLACK; // default color
            }
        }

        Color getColor() {
            return color;
        }

        float getOffset() {
            return offset;
        }
    }

    static class ClipPath extends Element {

        static final int CP_USER_SPACE_ON_USE = 0;
        static final int CP_OBJECT_BOUNDING_BOX = 1;
        private int clipPathUnits = CP_USER_SPACE_ON_USE;
        private Shape clipShape;

        protected void build() {
            super.build();
            String cpu = getString("clipPathUnits");
            clipPathUnits = "objectBoundingBox".equalsIgnoreCase(cpu) ? CP_OBJECT_BOUNDING_BOX : CP_USER_SPACE_ON_USE;
        }

        int getClipPathUnits() {
            return clipPathUnits;
        }

        Shape getClipPathShape() {
            if (getChildren().size() == 0) {
                return null;
            }
            if (clipShape == null) {
                if (getChildren().size() == 1) {
                    clipShape = ((ShapeElement)getChildren().get(0)).getShape();
                } else {
                    Area clipArea = null;
                    for (Iterator i = getChildren().iterator(); i.hasNext();) {
                        ShapeElement se = (ShapeElement)i.next();
                        Shape shape = se.getShape();
                        assert shape != null;
                        Area a = shape instanceof Area ? (Area)shape : new Area(shape);
                        if (clipArea == null) {
                            clipArea = a;
                        } else {
                            clipArea.intersect(a);
                        }
                    }
                    clipShape = clipArea;
                }
            }
            return clipShape;
        }

    }

    static class Use extends ShapeElement {

        private float x;
        private float y;
        private float width = 1f;
        private float height = 1f;
        private Element element;
        private AffineTransform refXform;
        private String href;

        protected void build() {
            super.build();
            x = getFloatUnits("x", 0);
            y = getFloatUnits("y", 0);
            width = getFloatUnits("width", 1);
            height = getFloatUnits("height", 1);
            href = getStyleHref();
            refXform = AffineTransform.getTranslateInstance(this.x, this.y);
        }

        protected void resolve() {
            super.resolve();
            if (href != null) {
                element = getRoot().get(href);
                if (element != null) {
                    element.resolve();
                    href = null;
                }
            }
        }

        void render(Graphics2D g) {
            beginLayer(g);
            AffineTransform saveXform = g.getTransform();
            g.transform(refXform);
            if (element == null || !(element instanceof RenderableElement)) {
                return;
            }
            RenderableElement re = (RenderableElement)element;
            re.pushParentContext(this);
            re.render(g);
            re.popParentContext();
            g.setTransform(saveXform);
            finishLayer(g);
        }

        Shape getShape() {
            if (element instanceof ShapeElement) {
                Shape shape = ((ShapeElement)element).getShape();
                shape = refXform.createTransformedShape(shape);
                shape = shapeToParent(shape);
                return shape;
            }
            return null;
        }

        Rectangle2D getBoundingBox() {
            if (element instanceof ShapeElement) {
                ShapeElement se = (ShapeElement)element;
                se.pushParentContext(this);
                Rectangle2D bounds = se.getBoundingBox();
                se.popParentContext();
                bounds = refXform.createTransformedShape(bounds).getBounds2D();
                bounds = boundsToParent(bounds);
                return bounds;
            }
            return null;
        }

        float getX() {
            return x;
        }

        float getY() {
            return y;
        }

        float getWidth() {
            return width;
        }

        float getHeight() {
            return height;
        }
    }

    static class Rect extends ShapeElement {

        private RectangularShape rect;

        protected void build() {
            super.build();
            float x = getFloatUnits("x", 0);
            float y = getFloatUnits("y", 0);
            float w = getFloatUnits("width", 0);
            float h = getFloatUnits("height", 0);
            float rx = getFloatUnits("rx", 0);
            float ry = getFloatUnits("ry", 0);
            if (getString("rx") == null) {
                rx = ry;
            }
            if (getString("ry") == null) {
                ry = rx;
            }
            if (rx == 0 && ry == 0) {
                rect = new Rectangle2D.Float(x, y, w, h);
            } else {
                rect = new RoundRectangle2D.Float(x, y, w, h, rx * 2, ry * 2);
            }
        }

        void render(Graphics2D g) {
            assert !(getParent() instanceof ClipPath);
            beginLayer(g);
            renderShape(g, rect);
            finishLayer(g);
        }

        Shape getShape() {
            return shapeToParent(rect);
        }

        Rectangle2D getBoundingBox() {
            return boundsToParent(includeStrokeInBounds(rect.getBounds2D()));
        }
    }

    static class LinearGradient extends Gradient {

        private float x1;
        private float y1;
        private float x2 = 1f;
        private float y2;

        protected void build() {
            super.build();
            x1 = getFloatUnits("x1", 0);
            y1 = getFloatUnits("y1", 0);
            x2 = getFloatUnits("x2", 1);
            y2 = getFloatUnits("y2", 0);
        }

        protected void resolve() {
            super.resolve();
            if (colors != null && colors.length == 1) {
                colors = new Color[] { colors[0], colors[0] };
                offsets = new float[] { offsets[0],  offsets[0] };
            }
        }

        Paint getPaint(Rectangle2D bounds, AffineTransform xform) {
            MultipleGradientPaint.CycleMethodEnum method;
            switch (getSpreadMethod()) {
                default:
                case SM_PAD:
                    method = MultipleGradientPaint.NO_CYCLE;
                    break;
                case SM_REPEAT:
                    method = MultipleGradientPaint.REPEAT;
                    break;
                case SM_REFLECT:
                    method = MultipleGradientPaint.REFLECT;
                    break;
            }
            Color[] colors = getStopColors();
            if (colors == null || colors.length < 2) {
//              throw new Error("LinearGradientPaint must specify at least 2 colors: " + colors.length);
//              System.err.println("WARNING: LinearGradientPaint must specify at least 2 colors");
                return null;
            }
            LinearGradientPaint paint;
            if (getGradientUnits() == GU_USER_SPACE_ON_USE) {
                paint = new LinearGradientPaint(new Point2D.Float(x1, y1), new Point2D.Float(x2, y2),
                        getStopOffsets(), colors, method, MultipleGradientPaint.SRGB,
                        getGradientTransform());
            } else {
                AffineTransform viewXform = new AffineTransform();
                viewXform.translate(bounds.getX(), bounds.getY());
                // This is a hack to get around shapes that have a width or height of 0.  Should be close enough to the true answer.
                double width = bounds.getWidth();
                double height = bounds.getHeight();
                if (width == 0) {
                    width = 1;
                }
                if (height == 0) {
                    height = 1;
                }
                viewXform.scale(width, height);
                viewXform.concatenate(getGradientTransform());
                paint = new LinearGradientPaint(new Point2D.Float(x1, y1), new Point2D.Float(x2, y2),
                        getStopOffsets(), colors, method, MultipleGradientPaint.SRGB, viewXform);
            }
            return paint;
        }
    }

    static class RadialGradient extends Gradient {

        private float cx = 0.5f;
        private float cy = 0.5f;
        private float fx = 0.5f;
        private float fy = 0.5f;
        private float r = 0.5f;

        protected void build() {
            super.build();
            cx = getFloatUnits("cx", 0.5f);
            cy = getFloatUnits("cy", 0.5f);
            fx = getFloatUnits("fx", 0.5f);
            fy = getFloatUnits("fy", 0.5f);
            r = getFloatUnits("r", 0.5f);
        }

        Paint getPaint(Rectangle2D bounds, AffineTransform xform) {
            MultipleGradientPaint.CycleMethodEnum method;
            switch (getSpreadMethod()) {
                default:
                case SM_PAD:
                    method = MultipleGradientPaint.NO_CYCLE;
                    break;
                case SM_REPEAT:
                    method = MultipleGradientPaint.REPEAT;
                    break;
                case SM_REFLECT:
                    method = MultipleGradientPaint.REFLECT;
                    break;
            }
            Color[] colors = getStopColors();
            if (colors == null || colors.length < 2) {
//              System.err.println("WARNING: RadialGradientPaint must specify at least 2 colors");
                return null;
            }
            if (r <= 0) {
                // iedesign/iedesign_Lightbulb_Grayscale.svg XMLID_12_ r="0"
//              System.err.println("WARNING: RadialGradientPaint radius must be > 0: " + getString("id"));
                return null;
            }
            RadialGradientPaint paint;
            if (getGradientUnits() == GU_USER_SPACE_ON_USE) {
                paint = new RadialGradientPaint(new Point2D.Float(cx, cy), r,
                        new Point2D.Float(fx, fy), getStopOffsets(), colors, method,
                        MultipleGradientPaint.SRGB, getGradientTransform());
            } else {
                AffineTransform viewXform = new AffineTransform();
                viewXform.translate(bounds.getX(), bounds.getY());
                viewXform.scale(bounds.getWidth(), bounds.getHeight());
                viewXform.concatenate(getGradientTransform());
                paint = new RadialGradientPaint(new Point2D.Float(cx, cy), r,
                        new Point2D.Float(fx, fy), getStopOffsets(), colors, method,
                        MultipleGradientPaint.SRGB, viewXform);
            }
            return paint;
        }
    }

    static class MissingGlyph extends ShapeElement {

        private Shape path;
        private int horizAdvX = -1;
        private int vertOriginX = -1;
        private int vertOriginY = -1;
        private int vertAdvY = -1;

        protected void build() {
            super.build();
            String commandList = getString("d");
            if (commandList != null) {
                String fillRule = getString("fill-rule", "nonzero");
                Path.PathCommand[] commands = Path.PathCommand.parsePathList(commandList);
                GeneralPath buildPath = new GeneralPath(
                        fillRule.equals("evenodd") ? GeneralPath.WIND_EVEN_ODD : GeneralPath.WIND_NON_ZERO,
                        commands.length);
                Path.BuildHistory hist = new Path.BuildHistory();
                for (int i = 0; i < commands.length; i++) {
                    Path.PathCommand cmd = commands[i];
                    cmd.appendPath(buildPath, hist);
                }
                AffineTransform at = AffineTransform.getScaleInstance(1, -1);
                path = at.createTransformedShape(buildPath);
            }
            horizAdvX = getInt("horiz-adv-x", -1);
            vertOriginX = getInt("vert-origin-x", -1);
            vertOriginY = getInt("vert-origin-y", -1);
            vertAdvY = getInt("vert-adv-y", -1);
        }

        Shape getPath() {
            return path;
        }

        void render(Graphics2D g) {
            if (path != null) {
                renderShape(g, path);
            }
            for (Iterator i = getChildren().iterator(); i.hasNext(); ) {
                Element e = (Element)i.next();
                if (e instanceof RenderableElement) {
                    ((RenderableElement)e).render(g);
                }
            }
        }

        int getHorizAdvX() {
            if (horizAdvX == -1) {
                horizAdvX = ((Font)getParent()).getHorizAdvX();
            }
            return horizAdvX;
        }

        int getVertOriginX() {
            if (vertOriginX == -1) {
                vertOriginX = getHorizAdvX() / 2;
            }
            return vertOriginX;
        }

        int getVertOriginY() {
            if (vertOriginY == -1) {
                vertOriginY = ((Font)getParent()).getFontFace().getAscent();
            }
            return vertOriginY;
        }

        int getVertAdvY() {
            if (vertAdvY == -1) {
                vertAdvY = ((Font)getParent()).getFontFace().getUnitsPerEm();
            }
            return vertAdvY;
        }

        Shape getShape() {
            if (path != null) {
                return shapeToParent(path);
            }
            return null;
        }

        Rectangle2D getBoundingBox() {
            if (path != null) {
                return boundsToParent(includeStrokeInBounds(path.getBounds2D()));
            }
            return null;
        }
    }

    static class Font extends Element {

        private int horizOriginX = 0;
        private int horizOriginY = 0;
        private int horizAdvX = -1;  // Must be specified
        private int vertOriginX = -1;  // Defaults to horizAdvX / 2
        private int vertOriginY = -1;  // Defaults to font's ascent
        private int vertAdvY = -1;  // Defaults to one 'em'.  See font-face
        private FontFace fontFace = null;
        private MissingGlyph missingGlyph = null;
        private final HashMap glyphs = new HashMap();

        protected void loaderAddChild(Element child) {
            super.loaderAddChild(child);
            if (child instanceof Glyph) {
                glyphs.put(((Glyph)child).getText(), child);
            } else if (child instanceof MissingGlyph) {
                missingGlyph = (MissingGlyph)child;
            } else if (child instanceof FontFace) {
                fontFace = (FontFace)child;
            }
        }

        protected void build() {
            super.build();
            horizOriginX = getInt("horiz-origin-x", 0);
            horizOriginY = getInt("horiz-origin-y", 0);
            horizAdvX = getInt("horiz-adv-x", -1);
            vertOriginX = getInt("vert-origin-x", -1);
            vertOriginY = getInt("vert-origin-y", -1);
            vertAdvY = getInt("vert-adv-y", -1);
            getRoot().registerFont(this);
        }

        FontFace getFontFace() {
            return fontFace;
        }

        MissingGlyph getGlyph(String s) {
            Glyph g = (Glyph)glyphs.get(s);
            return g == null ? missingGlyph : g;
        }

        int getHorizOriginX() {
            return horizOriginX;
        }

        int getHorizOriginY() {
            return horizOriginY;
        }

        int getHorizAdvX() {
            return horizAdvX;
        }

        int getVertOriginX() {
            if (vertOriginX != -1) {
                return vertOriginX;
            }
            vertOriginX = getHorizAdvX() / 2;
            return vertOriginX;
        }

        int getVertOriginY() {
            if (vertOriginY != -1) {
                return vertOriginY;
            }
            vertOriginY = fontFace.getAscent();
            return vertOriginY;
        }

        int getVertAdvY() {
            if (vertAdvY != -1) {
                return vertAdvY;
            }
            vertAdvY = fontFace.getUnitsPerEm();
            return vertAdvY;
        }

    }

    static class FontFace extends Element {

        private String fontFamily;
        private int unitsPerEm = 1000;
        private int ascent = -1;
        private int descent = -1;
        private int accentHeight = -1;
        private int underlinePosition = -1;
        private int underlineThickness = -1;
        private int strikethroughPosition = -1;
        private int strikethroughThickness = -1;
        private int overlinePosition = -1;
        private int overlineThickness = -1;

        FontFace() {
        }

        protected void build() {
            super.build();
            fontFamily = getString("font-family", "San Serif");
            unitsPerEm = getInt("units-per-em", 1000);
            ascent = getInt("ascent", -1);
            descent = getInt("descent", -1);
            accentHeight = getInt("accent-height", -1);
            descent = getInt("descent", -1);
            underlinePosition = getInt("underline-position", -1);
            underlineThickness = getInt("underline-thickness", -1);
            strikethroughPosition = getInt("strikethrough-position", -1);
            strikethroughThickness = getInt("strikethrough-thickenss", -1);
            overlinePosition = getInt("overline-position", -1);
            overlineThickness = getInt("overline-thickness", -1);
        }

        String getFontFamily() {
            return fontFamily;
        }

        int getUnitsPerEm() {
            return unitsPerEm;
        }

        int getAscent() {
            if (ascent == -1) {
                if (((Font)getParent()).vertOriginY == -1) {
                    ascent = 0;
                } else {
                    ascent = unitsPerEm - ((Font)getParent()).getVertOriginY();
                }
            }
            return ascent;
        }

        int getDescent() {
            if (descent == -1) {
                descent = ((Font)getParent()).getVertOriginY();
            }
            return descent;
        }

        int getAccentHeight() {
            if (accentHeight == -1) {
                accentHeight = getAscent();
            }
            return accentHeight;
        }

        int getUnderlinePosition() {
            if (underlinePosition == -1) {
                underlinePosition = unitsPerEm * 5 / 6;
            }
            return underlinePosition;
        }

        int getUnderlineThickness() {
            if (underlineThickness == -1) {
                underlineThickness = unitsPerEm / 20;
            }
            return underlineThickness;
        }

        int getStrikethroughPosition() {
            if (strikethroughPosition == -1) {
                strikethroughPosition = unitsPerEm * 3 / 6;
            }
            return strikethroughPosition;
        }

        int getStrikethroughThickness() {
            if (strikethroughThickness == -1) {
                strikethroughThickness = unitsPerEm / 20;
            }
            return strikethroughThickness;
        }

        int getOverlinePosition() {
            if (overlinePosition == -1) {
                overlinePosition = unitsPerEm * 5 / 6;
            }
            return overlinePosition;
        }

        int getOverlineThickness() {
            if (overlineThickness == -1) {
                overlineThickness = unitsPerEm / 20;
            }
            return overlineThickness;
        }
    }

    static abstract class Gradient extends SVG.FillElement {

        static final int SM_PAD = 0;
        static final int SM_REPEAT = 1;
        static final int SM_REFLECT = 2;
        private int spreadMethod = SM_PAD;
        static final int GU_OBJECT_BOUNDING_BOX = 0;
        static final int GU_USER_SPACE_ON_USE = 1;
        private  int gradientUnits = GU_OBJECT_BOUNDING_BOX;
        private final ArrayList stops = new ArrayList();
        private AffineTransform gradientTransform = GraphicsUtil.IDENTITY;
        protected float[] offsets;
        protected Color[] colors;
        private Gradient ref;
        private String href;

        protected void loaderAddChild(Element child) {
            super.loaderAddChild(child);
            if (!(child instanceof SVG.Stop)) {
                return;
            }
            appendStop((SVG.Stop)child);
        }

        protected void build() {
            super.build();
            String sm = getString("spreadMethod");
            if (sm != null) {
                sm = sm.toLowerCase();
                if ("repeat".equals(sm)) {
                    spreadMethod = SM_REPEAT;
                } else if ("reflect".equals(sm)) {
                    spreadMethod = SM_REFLECT;
                } else {
                    spreadMethod = SM_PAD;
                }
            }
            String gu = getString("gradientUnits");
            if (gu != null) {
                if ("userspaceonuse".equalsIgnoreCase(gu)) {
                    gradientUnits = GU_USER_SPACE_ON_USE;
                } else {
                    gradientUnits = GU_OBJECT_BOUNDING_BOX;
                }
            }
            String gt = getString("gradientTransform");
            if (gt != null) {
                gradientTransform = parseTransform(gt);
            }
            if (gradientTransform == null) {
                gradientTransform = GraphicsUtil.IDENTITY;
            }
            href = getStyleHref();
        }

        protected void resolve() {
            super.resolve();
            if (href != null) { // forward references:
                Object obj = getRoot().get(href);
                ref = (Gradient)(obj != null && obj instanceof Gradient ? obj : null);
                if (ref != null) {
                    ref.resolve();
                    href = null;
                }
            }
            processStops();
        }

        private void processStops() {
            Gradient g = ref;
            ArrayList s = stops;
            for (;;) {
                if (s != null && s.size() > 0) {
                    offsets = new float[s.size()];
                    colors = new Color[s.size()];
                    int ix = 0;
                    boolean nullColors = false;
                    for (Iterator i = s.iterator(); i.hasNext();) {
                        SVG.Stop stop = (SVG.Stop)i.next();
                        stop.resolve();
                        float o = stop.getOffset();
                        Color c = stop.getColor();
                        offsets[ix] = o;
                        colors[ix] = c;
                        if (ix > 0) {
                            int k = ix;
                            while (k > 0 &&  offsets[k - 1] > offsets[k]) {
                                // SVG_snapshot/Anonymous/Anonymous_Spybot.svg
                                // swap:
                                o = offsets[k];
                                offsets[k] = offsets[k - 1];
                                offsets[k - 1] = o;

                                c = colors[k];
                                colors[k] = colors[k - 1];
                                colors[k - 1] = c;
                                k--;
                            }
                        }
                        nullColors = nullColors || (c == null);
                        ix++;
                    }
                    // It is necessary that at least two stops defined to have a gradient effect.
                    // ... If one stop is defined, then paint with the solid color fill using
                    // the color defined for that gradient stop.
                    if (nullColors && colors.length > 1) {
                        Color nnc = null;
                        for (int i = 0; i < colors.length; i++) {
                            if (colors[i] != null) {
                                nnc = colors[i];
                                break;
                            }
                        }
                        for (int i = 0; i < colors.length; i++) {
                            if (colors[i] == null) {
                                colors[i] = nnc;
                            }
                        }
                    }
                }
                if (g == null) {
                    return;
                }
                s = g.stops;
                g = g.ref;
            }
        }


        float[] getStopOffsets() {
            return offsets;
        }

        Color[] getStopColors() {
            return colors;
        }

        private void appendStop(SVG.Stop stop) {
            stops.add(stop);
        }

        int getSpreadMethod() {
            return spreadMethod;
        }

        AffineTransform getGradientTransform() {
            return gradientTransform;
        }

        int getGradientUnits() {
            return gradientUnits;
        }
    }

    static class Pattern extends SVG.FillElement {

        static final int GU_OBJECT_BOUNDING_BOX = 0;
        static final int GU_USER_SPACE_ON_USE = 1;
        private int gradientUnits = -1;
        private float x = Float.NaN;
        private float y = Float.NaN;
        private float width = Float.NaN;
        private float height = Float.NaN;
        private AffineTransform patternXform = GraphicsUtil.IDENTITY;
        private Rectangle2D.Float viewBox;
        private Paint texPaint;
        private String href;
        private Pattern ref;

        protected void build() {
            super.build();
            String gu = getString("gradientUnits");
            if ("userspaceonuse".equalsIgnoreCase(gu)) {
                gradientUnits = GU_USER_SPACE_ON_USE;
            } if ("objectBoundingBox".equalsIgnoreCase(gu)) {
                gradientUnits = GU_OBJECT_BOUNDING_BOX;
            }
            String pt = getString("patternTransform");
            if (pt != null) {
                patternXform = parseTransform(pt);
            }
            x = getFloatUnits("x", 0);
            y = getFloatUnits("y", 0);
            width = getFloatUnits("width", Float.NaN);
            height = getFloatUnits("height", Float.NaN);
            float[] vb = getFloats("viewBox");
            if (vb != null) {
                viewBox = new Rectangle2D.Float(vb[0], vb[1], vb[2], vb[3]);
            }
            href = getStyleHref();
        }

        protected void resolve() {
            super.resolve();
            if (href != null) {
                ref = (Pattern)getRoot().get(href);
                if (ref != null) {
                    ref.resolve();
                    href = null;
                    processChain();
                }
            }
            preparePattern();
        }

        private void processChain() {
            Pattern p = ref;
            while (p != null) {
                if (gradientUnits == -1) {
                   gradientUnits = p.gradientUnits;
                }
                if (Float.isNaN(x)) {
                   x = p.x;
                }
                if (Float.isNaN(y)) {
                   y = p.y;
                }
                if (Float.isNaN(width)) {
                   width = p.width;
                }
                if (Float.isNaN(height)) {
                   height = p.height;
                }
                if (viewBox == null) {
                    viewBox = p.viewBox;
                }
                if (patternXform.isIdentity()) {
                    patternXform = p.patternXform;
                }
                p = p.ref;
            }
            if (gradientUnits == -1) {
               gradientUnits = GU_OBJECT_BOUNDING_BOX;
            }
            if (Float.isNaN(x)) {
               x = 0;
            }
            if (Float.isNaN(y)) {
               y = 0;
            }
            if (Float.isNaN(width)) {
               width = 0;
            }
            if (Float.isNaN(height)) {
               height = 0;
            }
        }

        private void preparePattern() {
            int tileWidth = Math.round(width);
            int tileHeight = Math.round(height);
            float stretchX = 1f, stretchY = 1f;
            if (!patternXform.isIdentity()) {
                // Scale our source tile so that we can have nice sampling from it.
                float xlateX = (float)patternXform.getTranslateX();
                float xlateY = (float)patternXform.getTranslateY();
                Point2D.Float pt = new Point2D.Float(), pt2 = new Point2D.Float();
                pt.setLocation(width, 0);
                patternXform.transform(pt, pt2);
                pt2.x -= xlateX;
                pt2.y -= xlateY;
                stretchX = (float)Math.sqrt(pt2.x * pt2.x + pt2.y * pt2.y) * 1.5f / width;
                // ???? pt.setLocation(height, 0);
                pt.setLocation(0, height);
                patternXform.transform(pt, pt2);
                pt2.x -= xlateX;
                pt2.y -= xlateY;
                stretchY = (float)Math.sqrt(pt2.x * pt2.x + pt2.y * pt2.y) * 1.5f / height;
                tileWidth = Math.round(width * stretchX);
                tileHeight = Math.round(height * stretchY);
            }
            int w = Math.max(1, tileWidth);
            int h = Math.max(1, tileHeight);
            BufferedImage buf;
            if (w == 1 && h == 1) {
                buf = get1x1();
            } else {
                buf = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = buf.createGraphics();
                g.setClip(0, 0, tileWidth, tileHeight);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (tileWidth != 0 || tileHeight != 0) {
                    for (Iterator i = getChildren().iterator(); i.hasNext();) {
                        Element e = (Element)i.next();
                        if (e instanceof SVG.RenderableElement) {
                            AffineTransform xform = new AffineTransform();
                            if (viewBox == null) {
                                xform.translate(-x, -y);
                            } else {
                                xform.scale(tileWidth / viewBox.width, tileHeight / viewBox.height);
                                xform.translate(-viewBox.x, -viewBox.y);
                            }
                            g.setTransform(xform);
                            ((SVG.RenderableElement)e).render(g);
                        }
                    }
                } else {
                    g.setColor(new Color(0, 0, 0, 0)); // transparent
                    g.fill(new Rectangle(0, 0, w, h));
                }
                g.dispose();
            }
            if (patternXform.isIdentity()) {
                texPaint = new TexturePaint(buf, new Rectangle2D.Float(x, y, width, height));
            } else {
                patternXform.scale(1 / stretchX, 1 / stretchY);
                texPaint = new PatternPaint(buf, patternXform);
            }
        }

        Paint getPaint(Rectangle2D bounds, AffineTransform xform) {
            return texPaint;
        }
    }

    static class PatternPaint implements Paint {

        private BufferedImage source;  //  Image we're rendering from
        private AffineTransform xform;

        PatternPaint(BufferedImage source, AffineTransform xform) {
            this.source = source;
            this.xform = xform;
        }

        public PaintContext createContext(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds,
                AffineTransform xform, RenderingHints hints) {
            return new PatternPaintContext(source, deviceBounds, xform, this.xform);
        }

        public int getTransparency() {
            return source.getColorModel().getTransparency();
        }

        private static class PatternPaintContext implements PaintContext {

            BufferedImage source;  //  Image we're rendering from
            Rectangle deviceBounds;  //  int size of rectangle we're rendering to
            AffineTransform xform;  //  distortion applied to this pattern
            int sourceWidth;
            int sourceHeight;
            BufferedImage buf;

            PatternPaintContext(BufferedImage source, Rectangle deviceBounds, AffineTransform userXform,
                    AffineTransform distortXform) {
                this.source = source;
                this.deviceBounds = deviceBounds;
                try {
                    xform = distortXform.createInverse();
                    xform.concatenate(userXform.createInverse());
                } catch (NoninvertibleTransformException e) {
                    throw new Error(e);
                }
                sourceWidth = source.getWidth();
                sourceHeight = source.getHeight();
            }

            public void dispose() {
            }

            public ColorModel getColorModel() {
                return source.getColorModel();
            }

            public Raster getRaster(int x, int y, int w, int h) {
//              System.err.println("PatternPaintContext.getRaster(" + x + "," + y + " " + w + "x" + h + ")");
                if (buf == null || buf.getWidth() != w || buf.getHeight() != buf.getHeight()) {
                    buf = new BufferedImage(w, h, source.getType());
                }
                Point2D.Float srcPt = new Point2D.Float(), destPt = new Point2D.Float();
                for (int j = 0; j < h; j++) {
                    for (int i = 0; i < w; i++) {
                        destPt.setLocation(i + x, j + y);
                        xform.transform(destPt, srcPt);
                        int ii = ((int)srcPt.x) % sourceWidth;
                        if (ii < 0) {
                            ii += sourceWidth;
                        }
                        int jj = ((int)srcPt.y) % sourceHeight;
                        if (jj < 0) {
                            jj += sourceHeight;
                        }
                        buf.setRGB(i, j, source.getRGB(ii, jj));
                    }
                }
                return buf.getData();
            }
        }
    }

}
