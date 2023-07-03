/* new data */
var x = d3.timeDays(new Date(2015, 06, 01), new Date(2020, 10, 30));
var y = Array.from({
    length: x.length
}, Math.random).map(n => Math.floor(n * 10) + 5);
var data = x.map((v, i) => {
    return {
        "date": v,
        "value": y[i]
    }
});

var margin = {
    top: 20,
    right: 20,
    bottom: 30,
    left: 40
}
var height = 440;
var width = 600;
var focusHeight = 100;

var focusedArea = d3.extent(x);

const svg = d3.select('#my_chart').append("svg")
    .attr("viewBox", [0, 0, width, height])
    .style("display", "block");

const clipId = {
    id: "clip"
};

const clip = svg.append("clipPath")
    .attr("id", clipId.id)
    .append("rect")
    .attr("x", margin.left)
    .attr("y", 0)
    .attr("height", height)
    .attr("width", width - margin.left - margin.right);

const gx = svg.append("g");

const gy = svg.append("g");

const path = svg.append("path")
    .datum(data)
    .attr("clip-path", `url(#${clipId.id})`)
    .attr("fill", "steelblue");

const updateChart = (focusX, focusY) => {
    gx.call(xAxis, focusX, height);
    gy.call(yAxis, focusY, data.y);
    path.attr("d", area(focusX, focusY));
};


/* begin observable code */
var focus = () => {
    const svg = d3.select("#focus").append("svg")
        .attr("viewBox", [0, 0, width, focusHeight])
        .style("display", "block");
    const brush = d3.brushX()
        .extent([
            [margin.left, 0.5],
            [width - margin.right, focusHeight - margin.bottom + 0.5]
        ])
        .on("brush", brushed)
        .on("end", brushended);

    const defaultSelection = [x(d3.utcYear.offset(x.domain()[1], -1)), x.range()[1]];

    svg.append("g")
        .call(xAxis, x, focusHeight);

    svg.append("path")
        .datum(data)
        .attr("fill", "steelblue")
        .attr("d", area(x, y.copy().range([focusHeight - margin.bottom, 4])));

    const gb = svg.append("g")
        .call(brush)
        .call(brush.move, defaultSelection);

    function brushed({
                         selection
                     }) {
        if (selection) {
            svg.property("value", selection.map(x.invert, x).map(d3.utcDay.round));
            svg.dispatch("input");
            focusedArea = svg.property('value');
            update();
        }
    }

    function brushended({
                            selection
                        }) {
        if (!selection) {
            gb.call(brush.move, defaultSelection);
        }
    }
    return svg.node();
}

var update = function() {
    const [minX, maxX] = focusedArea;
    const maxY = d3.max(data, d => minX <= d.date && d.date <= maxX ? d.value : NaN);
    updateChart(x.copy().domain(focusedArea), y.copy().domain([0, maxY]));
}

var area = (x, y) => d3.area()
    .defined(d => !isNaN(d.value))
    .x(d => x(d.date))
    .y0(y(0))
    .y1(d => y(d.value))

var x = d3.scaleUtc()
    .domain(d3.extent(data, d => d.date))
    .range([margin.left, width - margin.right])

var y = d3.scaleLinear()
    .domain([0, d3.max(data, d => d.value)])
    .range([height - margin.bottom, margin.top])

var xAxis = (g, x, height) => g
    .attr("transform", `translate(0,${height - margin.bottom})`)
    .call(d3.axisBottom(x).ticks(width / 80).tickSizeOuter(0))

var yAxis = (g, y, title) => g
    .attr("transform", `translate(${margin.left},0)`)
    .call(d3.axisLeft(y))
    .call(g => g.select(".domain").remove())
    .call(g => g.selectAll(".title").data([title]).join("text")
        .attr("class", "title")
        .attr("x", -margin.left)
        .attr("y", 10)
        .attr("fill", "currentColor")
        .attr("text-anchor", "start")
        .text(title))




focus();
