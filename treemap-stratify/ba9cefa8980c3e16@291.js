import define1 from "./a33468b95d0b15b0@817.js";

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

function subtractMonths(date, months) {
    date.setMonth(date.getMonth() - months);
    return date;
}

function _chart(d3, flare, tile, DOM, complexity, statements, projects, startdate) {
    console.log(flare, complexity, statements, projects);
    flare = d3.filter(flare, i =>
        i.complexity > complexity &&
        i.size > statements &&
        i.epoch > startdate
    );

    if (projects.length > 0) {
        const setOfProjects = new Set(projects);
        flare = d3.filter(flare, i => setOfProjects.has(i.project));
    }

    const methods = flare.length;
    document.querySelector(".methods").innerText = methods;

    const sumOfStatements = d3.sum(flare, d => d.size);
    document.querySelector(".statements").innerText = sumOfStatements;

    const sumOfComplexity = d3.sum(flare, d => d.complexity);
    document.querySelector(".complexity").innerText = sumOfComplexity;

    // console.log(flare);

    // Stratify.
    const data = d3.stratify().path(d => d.name.replace(/\./g, "/"))(flare);
    // console.log(data);

    // Specify the chart’s dimensions.
    const width = 1700;
    const height = 800;

    // Specify the color scale.
    // const color = d3.scaleOrdinal(data.children.map(d => d.id.split("/").at(5)), d3.schemeTableau10);
    const color = d3.scaleOrdinal(data.children.map(d => d.data?.project), d3.schemeTableau10);

    // Compute the layout.
    const root = d3.treemap()
        .tile(tile) // e.g., d3.treemapSquarify
        .size([width, height])
        .padding(1)
        .round(true)
        (d3.hierarchy(data)
            .sum(d => d.data?.size)
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
        .attr("transform", d => `translate(${d.x0},${d.y0})`)
        .attr("href", d => `http://pdbitbucket01:7990/projects/EQA-SPLIT/repos/${d.data.data.project}/browse/${d.data.data.fullPath}#${d.data.data.lineStart}`)
        .attr("target", "_blank");

    // Append a tooltip.
    const format = d3.format(",d");
    leaf.append("title")
        .text(d => `${d.data.id.slice(1).replace(/\//g, ".")}\n${format(d.value)}`);

    const max = d3.max(flare, d => d.complexity);
    const opacity = d3.scaleLinear([0, max], [0.2, 1]);

    // Append a color rectangle.
    leaf.append("rect")
        .attr("id", d => (d.leafUid = DOM.uid("leaf")).id)
        .attr("fill", d => color(d.data.data.project))
        .attr("fill-opacity", d => opacity(d.data.data.complexity))
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
        .data(d => d.data.id
            .split("/")
            .at(-1)
            .split(/(?=[A-Z][a-z])|\s+/g)
            .concat(format(d.value))
            .concat(format(d.data.data.complexity))
        )
        .join("tspan")
        .attr("x", 3)
        .attr("y", (d, i, nodes) => `${(i === nodes.length - 1) * 0.3 + 1.1 + i * 0.9}em`)
        .attr("fill-opacity", (d, i, nodes) => i === nodes.length - 1 ? 0.7 : null)
        .text(d => d);

    svg.call(zoom);

    function zoomed(event) {
        // console.log(event.transform);
        const {transform} = event;
        g.attr("transform", transform);
        text.attr("font-size", (1 / transform.k) * 10 + "px");
        // g.attr("stroke-width", 1 / transform.k);
    }


    return Object.assign(svg.node(), {scales: {color}});
}


async function _flare(FileAttachment) {
    const data = await FileAttachment("data.csv").csv({typed: true});
    console.log(data);
    return data;
}

export default async function define(runtime, observer) {
    const main = runtime.module();

    function toString() {
        return this.url;
    }

    const fileAttachments = new Map([
        ["data.csv", {url: new URL("./files/data.csv", import.meta.url), mimeType: "text/csv", toString}],
        ["projects.csv", {url: new URL("./files/projects.csv", import.meta.url), mimeType: "text/csv", toString}]
    ]);

    main.builtin("FileAttachment", runtime.fileAttachments(name => fileAttachments.get(name)));

    main.variable(observer("projects")).define("projects", ["Generators", "viewof projects"], (G, c) => G.input(c));
    main.variable(observer("viewof projects")).define("viewof projects", ["Inputs", "d3"],
        async (Inputs, d3) => {
            const data = await d3.csv("files/projects.csv");
            const projects = data.map(d => d.name);
            return Inputs.checkbox(projects, {label: "Projects"})
        });

    // console.log(_flare);
    main.variable(observer("viewof startdate")).define("viewof startdate", ["Inputs"], (Inputs) => Inputs.date({label: "Date", value: subtractMonths(new Date(), 4)}));
    main.variable(observer("startdate")).define("startdate", ["Generators", "viewof startdate"], (G, c) => G.input(c));

    main.variable(observer("viewof complexity")).define("viewof complexity", ["Inputs", "d3"], (Inputs, d3) => Inputs.range([0, 500], {label: "Complexity", step: 5, value: 0}));
    main.variable(observer("complexity")).define("complexity", ["Generators", "viewof complexity"], (G, c) => G.input(c));

    main.variable(observer("viewof statements")).define("viewof statements", ["Inputs", "d3"], (Inputs, d3) => Inputs.range([0, 500], {label: "Statements", step: 5, value: 0}));
    main.variable(observer("statements")).define("statements", ["Generators", "viewof statements"], (G, c) => G.input(c));

    main.variable(observer("viewof tile")).define("viewof tile", ["Inputs", "d3"], _tile);
    main.variable(observer("tile")).define("tile", ["Generators", "viewof tile"], (G, _) => G.input(_));

    main.variable(observer("key")).define("key", ["Swatches", "chart"], _key);

    main.variable(observer("chart")).define("chart", ["d3", "flare", "tile", "DOM", "complexity", "statements", "projects", "startdate"], _chart);
    main.variable(observer("flare")).define("flare", ["FileAttachment"], _flare);

    const child1 = runtime.module(define1);
    main.import("Swatches", child1);

    return main;
}
