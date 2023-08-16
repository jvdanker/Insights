import define1 from "./a33468b95d0b15b0@817.js";

function _1(md) {
    return (
        md`# Treemap, CSV

Use [d3.stratify](https://d3js.org/d3-hierarchy/stratify) to compute a [treemap](/@d3/treemap/2?intent=fork) from a flat list of references in CSV format, instead of a hierarchical JSON format.`
    )
}

function _tile(Inputs, d3) {
    return (
        Inputs.select(
            new Map([
                ["binary", d3.treemapBinary],
                ["squarify", d3.treemapSquarify],
                ["slice-dice", d3.treemapSliceDice],
                ["slice", d3.treemapSlice],
                ["dice", d3.treemapDice]
            ]),
            {label: "Tiling method", value: d3.treemapBinary}
        )
    )
}

function _key(Swatches, chart) {
    return (
        Swatches(chart.scales.color)
    )
}

function _chart(d3, flare, tile, DOM) {
    // Stratify.
    const data = d3.stratify().path(d => d.name.replace(/\./g, "/"))(flare);

    // Specify the chart’s dimensions.
    const width = 1154;
    const height = 1154;

    // Specify the color scale.
    const color = d3.scaleOrdinal(data.children.map(d => d.id.split("/").at(5)), d3.schemeTableau10);

    // Compute the layout.
    const root = d3.treemap()
        .tile(tile) // e.g., d3.treemapSquarify
        .size([width, height])
        .padding(1)
        .round(true)
        (d3.hierarchy(data)
            .sum(d => d.data.size)
            .sort((a, b) => b.value - a.value));

    const zoom = d3.zoom()
        .scaleExtent([1, 8])
        .on("zoom", zoomed);

    // Create the SVG container.
    const svg = d3.create("svg")
        .attr("viewBox", [0, 0, width, height])
        .attr("width", width)
        .attr("height", height)
        .attr("style", "max-width: 100%; height: auto; font: 10px sans-serif;");

    const g = svg.append("g");

    // Add a cell for each leaf of the hierarchy, with a link to the corresponding GitHub page.
    const leaf = g.selectAll("g")
        .data(root.leaves())
        .join("a")
        .attr("transform", d => `translate(${d.x0},${d.y0})`);
        // .attr("href", d => `${d.data.id}`)
        // .attr("target", "_blank");

    // Append a tooltip.
    const format = d3.format(",d");
    leaf.append("title")
        .text(d => `${d.data.id.slice(1).replace(/\//g, ".")}\n${format(d.value)}`);

    // Append a color rectangle.
    leaf.append("rect")
        .attr("id", d => (d.leafUid = DOM.uid("leaf")).id)
        .attr("fill", d => color(d.data.id.split("/").at(5)))
        .attr("fill-opacity", 0.6)
        .attr("width", d => d.x1 - d.x0)
        .attr("height", d => d.y1 - d.y0);

    // Append a clipPath to ensure text does not overflow.
    leaf.append("clipPath")
        .attr("id", d => (d.clipUid = DOM.uid("clip")).id)
        .append("use")
        .attr("xlink:href", d => d.leafUid.href);

    // Append multiline text. The last line shows the value and has a specific formatting.
    const text = leaf.append("text")
        .attr("clip-path", d => d.clipUid)
        .selectAll("tspan")
        .data(d => d.data.id.split("/").at(-1).split(/(?=[A-Z][a-z])|\s+/g).concat(format(d.value)))
        .join("tspan")
        .attr("x", 3)
        .attr("y", (d, i, nodes) => `${(i === nodes.length - 1) * 0.3 + 1.1 + i * 0.9}em`)
        .attr("fill-opacity", (d, i, nodes) => i === nodes.length - 1 ? 0.7 : null)
        .text(d => d);

    svg.call(zoom);

    function zoomed(event) {
        console.log(event.transform);
        const {transform} = event;
        g.attr("transform", transform);
        text.attr("font-size", (1 / transform.k) * 10 + "px");
        // g.attr("stroke-width", 1 / transform.k);
    }


    return Object.assign(svg.node(), {scales: {color}});
}


function _flare(FileAttachment) {
    return (
        FileAttachment("flare.csv").csv({typed: true})
    )
}

export default function define(runtime, observer) {
    const main = runtime.module();

    function toString() {
        return this.url;
    }

    const fileAttachments = new Map([
        ["flare.csv", {url: new URL("./files/data.csv", import.meta.url), mimeType: "text/csv", toString}]
    ]);
    main.builtin("FileAttachment", runtime.fileAttachments(name => fileAttachments.get(name)));
    main.variable(observer()).define(["md"], _1);
    main.variable(observer("viewof tile")).define("viewof tile", ["Inputs", "d3"], _tile);
    main.variable(observer("tile")).define("tile", ["Generators", "viewof tile"], (G, _) => G.input(_));
    main.variable(observer("key")).define("key", ["Swatches", "chart"], _key);
    main.variable(observer("chart")).define("chart", ["d3", "flare", "tile", "DOM"], _chart);
    main.variable(observer("flare")).define("flare", ["FileAttachment"], _flare);
    const child1 = runtime.module(define1);
    main.import("Swatches", child1);
    return main;
}
