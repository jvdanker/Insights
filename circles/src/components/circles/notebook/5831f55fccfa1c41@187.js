function _chart(d3, data) {

    // Specify the chart’s dimensions.
    const width = 1200;
    const height = 1200;

    // Create the color scale.
    const color = d3.scaleLinear()
        .domain([0, 20])
        .range(["hsl(152,95%,95%)", "hsl(228,30%,40%)"])
        .interpolate(d3.interpolateHcl);

    const colorComplexity = d3
        .scaleSequential(d3.interpolatePlasma)
        .domain([0,100]);

    const pack = data => d3.pack()
        .size([width, height])
        .padding(3)(d3.hierarchy(data)
            .sum(d => d.value)
            .sort((a, b) => b.value - a.value));
    const root = pack(data);

    // Create the SVG container.
    const svg = d3.create("svg")
        .attr("viewBox", `-${width / 2} -${height / 2} ${width} ${height}`)
        .attr("width", width)
        .attr("height", height)
        .attr("style", `max-width: 100%; height: auto; display: block; margin: 0 -14px; background: ${color(0)}; cursor: pointer;`);

    // Append the nodes.
    const node = svg.append("g")
        .selectAll("circle")
        .data(root.descendants().slice(1))
        .join("circle")
        .attr("fill", d => d.children ? color(d.depth) : colorComplexity(d.data.complexity))
        .attr("fill-opacity", "0.4")
        // .attr("pointer-events", d => !d.children ? "none" : null)
        .on("mouseover", function (e, d) {
            // console.log(d.data);
            d3.select(this).attr("stroke", "#000");
        })
        .on("mouseout", function () {
            d3.select(this).attr("stroke", null);
        })
        .on("click", (event, d) => focus !== d && (zoom(event, d), event.stopPropagation()));

    // Append the text labels.
    const label = svg.append("g")
        .style("font", "18px sans-serif")
        .attr("pointer-events", "none")
        .attr("text-anchor", "middle")
        .selectAll("text")
        .data(root.descendants())
        .join("text")
        .style("fill-opacity", d => d.parent === root ? 1 : 0)
        .style("fill", d => d.children ? "black" : "white")
        .style("display", "inline")
        // .style("display", d => d.parent === root ? "inline" : "none")
        .text(d => d.data.name + (d.data.complexity ? " (" + d.data.complexity + ")" : ""));

    // Create the zoom behavior and zoom immediately in to the initial focus node.
    svg.on("click", (event) => zoom(event, root));
    let focus = root;
    let view;
    zoomTo([focus.x, focus.y, focus.r * 2]);

    function zoomTo(v) {
        const k = width / v[2];

        view = v;

        label.attr("transform", d => `translate(${(d.x - v[0]) * k},${(d.y - v[1]) * k})`);
        node.attr("transform", d => `translate(${(d.x - v[0]) * k},${(d.y - v[1]) * k})`);
        node.attr("r", d => d.r * k);
    }

    function zoom(event, d) {
        const focus0 = focus;

        focus = d;

        const transition = svg.transition()
            .duration(event.altKey ? 7500 : 750)
            .tween("zoom", d => {
                const i = d3.interpolateZoom(view, [focus.x, focus.y, focus.r * 2]);
                return t => zoomTo(i(t));
            });

        label
            .filter(function (d) {
                return d.parent === focus || this.style.display === "inline";
            })
            .transition(transition)
            .style("fill-opacity", d => d.parent === focus ? 1 : 0)
            .on("start", function (d) {
                if (d.parent === focus) this.style.display = "inline";
            })
            .on("end", function (d) {
                if (d.parent !== focus) this.style.display = "none";
            });
    }

    return svg.node();
}

function _data(FileAttachment) {
    return (FileAttachment("flare-2.json").json())
}

export default function define(runtime, observer) {
    debugger;
    const main = runtime.module();

    function toString() {
        return this.url;
    }

    const fileAttachments = new Map([["flare-2.json", {
        url: new URL("./files/data.json", import.meta.url),
        mimeType: "application/json",
        toString
    }]]);

    main.builtin("FileAttachment", runtime.fileAttachments(name => fileAttachments.get(name)));
    // main.variable(observer()).define(["md"], _1);
    main.variable(observer("chart")).define("chart", ["d3", "data"], _chart);
    main.variable(observer("data")).define("data", ["FileAttachment"], _data);

    return main;
}
