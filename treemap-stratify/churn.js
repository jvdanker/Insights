import define1 from "./churn-inputs.js";

// function _1(md){return(
//     md`# RF Energy Scatterplot
// Ten minutes of radio frequency energy data from an RF sensor on Mt. Everest on 12/31/2012.
//
// Drag the datawidth slider to see more of the data; hover for details.
// `
// )}

function _chart(d3,DOM,width,height){return(
    d3.select(DOM.svg(width, height))
        .attr('style', 'border: 1px solid #f0f0f0')
        .node()
)}

// function _start(slider){return(
//     slider({
//         min: 0,
//         max: 9999,
//         step: 1,
//         value: 0,
//         title: "start",
//         description: "offsets into data array"
//     })
// )}

// function _datawidth(slider){return(
//     slider({
//         min: 2,
//         max: 9999,
//         step: 1,
//         value: 9999,
//         title: "datawidth",
//         description: "number of data array element to show at once"
//     })
// )}

// function _5(md){return(
//     md`
// - Axes labels
// - Updating scales
// - An updating time scale with milli-seconds
// - Color and size legends
// - Div-based tooltip
// `
// )}

async function _csvdata(d3,FileAttachment){return(
    d3.csvParse(await FileAttachment("example_data2.csv").text())
        .map(d => ({
            churn: +d.churn,
            statements: +d.statements,
            complexity: +d.complexity,
            project: d.project,
            className: d.className,
            fullPath: d.fullPath,
            epoch: new Date(d.epoch)
        }))
)}

function _initialSlice(csvdata){return(
    csvdata.slice(0,9999)
)}

// function _subset(csvdata,start,datawidth){return(
//     csvdata.slice(start, start+datawidth)
// )}

function _9(d3,chart,width,margin,height,d3legend,colorScale,rScale,setScale,initialSlice,initAxis,md,update)
{
    let g = d3.select(chart).append("g")

    g
        .attr("width", width - (margin.left + margin.right))
        .attr("height", height - (margin.top + margin.bottom))
        .attr('id', 'vis')
        .attr("transform", "translate(" + margin.left + "," + margin.top + ")")

        .append('rect')
        .attr("width", width - (margin.left + margin.right))
        .attr("height", height - (margin.top + margin.bottom))
        .attr('fill', 'none')

    // colorLegend
    let colorLegend = d3legend.legendColor(colorScale)
        .labelFormat(d3.format(".0f"))
        .scale(colorScale)
        .shape("path", d3.symbol().type(d3.symbolCircle).size(150)())
        .shapePadding(5)
        .shapeWidth(50)
        .shapeHeight(20)
        .labelOffset(12)
        .title('Complexity')
        .titleWidth(100);

    d3.select(chart).append("g")
        .attr('font-family','Roboto')
        .attr('font-size', 14)
        .attr("transform", "translate(" + (width - margin.right+12) + "," + (20) + ")")
        .call(colorLegend);

    // size Legend
    var sizeLegend = d3legend.legendSize()
        .scale(rScale)
        .title('Statements')
        .shape('circle')
        .shapePadding(15)
        //.shapeRadius(15)
        .labelOffset(20)
        .labelFormat(d3.format(".3s"))
        .orient('vertical');

    d3.select(chart).append("g")
        .attr('font-family','Roboto')
        .attr('font-size', 14)
        .attr('fill-opacity', 0.6)
        .attr("transform", "translate(" + (width - margin.right+12) + "," + (height/2 + 24) + ")")
        .call(sizeLegend);

    // tooltip
    d3
        .select('body')
        .append('div')
        .attr('id', 'tooltip')
        .attr('style', 'border: none')
        .style('border-radius', '10px')
        .style('box-shadow', '4px 4px 5px lightgrey')
        .style('background', 'white')
        .style("position", "absolute")
        .style("z-index", "10")
        .style('font-size', '14')
        .style('font-family', 'Roboto')
        .style('padding', '10px 10px 10px 10px')
        .style('opacity', 0);

    setScale(initialSlice)
    update(initialSlice)
    initAxis()
}


// function _10(setScale,subset,update,md)
// {
//     setScale(subset)
//     update(subset)
//
//     return md`Update when subset changes`
// }


// function _11(md){return(
//     md`### Functions`
// )}

function _opacityScale(d3, csvdata) {
    const domain = d3.extent(csvdata, d => d.epoch);
    const range = [0.1,1];
    const scale = d3.scaleTime(domain, range);
    return scale;
}

function _colorScale(d3,csvdata) {
    const max = d3.max(csvdata, d => d.complexity);
    const min = d3.min(csvdata, d => d.complexity);
    return d3.scaleSequential(d3.interpolatePlasma).domain([min,max]);

    // const projects = csvdata.map(d => d.project);
    // return d3.scaleOrdinal(projects, d3.schemeTableau10);
}

function _rScale(d3,csvdata){return(
    d3.scaleSqrt()
        .domain(d3.extent(csvdata, d => d.statements))
        .range([2, 18])
)}

function _initAxis(d3,chart,margin,height,xAxisCall,width,yAxisCall){return(
    function initAxis() {
        let vis = d3.select(chart)
        vis.append("g")
            .attr("id", "xaxis")
            .attr("transform", "translate(" + margin.left + "," + (height - margin.bottom) + ")")
            .call(xAxisCall)

        // x axis label
        vis.append("text")
            .attr("transform",
                "translate(" + (width/2) + " ," +
                (height - margin.bottom + margin.top + 30) + ")")
            .style("text-anchor", "middle")
            .text("Churn")
            .attr('font-family', 'Roboto');

        vis.append("g")
            .attr("id", "yaxis")
            .attr("transform", "translate(" + [margin.left, margin.top] + ")")
            .call(yAxisCall)

        // y axis label
        vis.append("text")
            //.attr('transform', 'rotate(-90)')
            .attr("transform", "translate(" + [margin.left-120, height/2] + ")")
            //.attr("x", margin.left - 30)
            // .attr("y",(height / 2))
            .text("Complexity")
            .attr('font-family', 'Roboto')
    }
)}

function _formatTime(d3){return(
    d3.timeFormat("%H:%M:%S")
)}

function _parseTime(d3){return(
    d3.timeParse("%Y-%m-%d %H:%M:%S.%f+00")
)}

function _setScale(d3,xScale,xAxisCall,yScale,yAxisCall){return(
    function setScale(data){
        let rgx = d3.extent(data, d => d.churn).reduce((a,b) => b-a)*0.1;
        xScale.domain(
            [
                0, //d3.min(data, d => d.Frequency)-rgx,
                d3.max(data, d => d.churn)+rgx
            ]
        ).nice();

        //xScale.range([margin.left, width-(margin.right+margin.left)])
        xAxisCall.scale(xScale)

        //yScale.domain(d3.extent(data, d => d.Date))
        let rgy = d3.extent(data, d => d.complexity).reduce((a,b) => b-a)*0.01;
        yScale.domain(
            [
                // d3.min(data, d => d3.timeMillisecond.offset(d.Date, -rgy)),
                // d3.max(data, d => d3.timeMillisecond.offset(d.Date, +rgy))
                // d3.min(data, d => d.complexity),
                0,
                d3.max(data, d => d.complexity)
            ]
        ).nice();

        yAxisCall.scale(yScale)
    }
)}

function _update(d3,xScale,yScale,rScale,opacityScale, colorScale,updateAxis){return(
    function update (data){
        var t = d3.transition()
            .duration(500)
            .ease(d3.easeLinear);

        // update the circles
        var us = d3.select('#vis').selectAll("circle")
            .data(data)

        var div = d3.select('#tooltip')

        us.enter()
            .append("circle") // Uses the enter().append() method
            .attr("cx", function(d, i) { return xScale(d.churn) })
            .attr("cy", function(d) { return yScale(d.complexity) })
            .style('cursor', 'crosshair')
            .on('click', d => {
                window.open(`http://pdbitbucket01:7990/projects/EQA-SPLIT/repos/${d.project}/browse/${d.fullPath}`, '_blank');
            })
            .on('mouseover', d => {
                div
                    .transition()
                    .duration(200)
                    .style('opacity', 1);
                div
                    .html(`
<table>
<tr><td>Project:</td><td>${d.project}</td></tr>
<tr><td>Class:</td><td>${d.className}</td></tr>
<tr><td>Statements:</td><td>${d.statements}</td></tr>
<tr><td>Complexity:</td><td>${d.complexity}</td></tr>
<tr><td>Churn:</td><td>${d.churn}</td></tr>
<tr><td>Last change:</td><td>${d.epoch}</td></tr>
</table>
`)
                    .style('left', d3.event.pageX + 18 + 'px')
                    .style('top', d3.event.pageY - 28 + 'px');
            })
            .on("mousemove", d => {
                div
                    .style("left", (d3.event.pageX+18) + "px")
                    .style("top", (d3.event.pageY-28) + "px");
            })
            .on('mouseout', () => {
                div
                    .transition()
                    .duration(500)
                    .style('opacity', 0);
            })

            .transition(t)
            .attr("cy", function(d) { return yScale(d.complexity) })
            .attr("r", function(d) { return rScale(d.statements) })
            .attr("fill", (d) => colorScale(d.complexity))
            .attr('fill-opacity', d => opacityScale(d.epoch))

        us
            .transition(t)
            .attr("cy", function(d) { return yScale(d.complexity) })
            .attr("r", function(d) { return rScale(d.statements) })
            .attr("fill", (d) => colorScale(d.complexity))
            .attr('fill-opacity', d => opacityScale(d.epoch))

        us.exit()
            .remove();

        updateAxis()
    }
)}

function _xAxisCall(d3){return(
    d3.axisBottom().tickFormat(d3.format(".3s"))
)}

function _yAxisCall(d3,formatTime){
    // d3.axisLeft().tickFormat(formatTime)
    return d3.axisLeft()
        .tickFormat(d3.format(".3s"))
}

function _xScale(d3,width,margin){return(
    d3.scaleLinear()
        //.domain(d3.extent(initialSlice, d => d.Frequency))
        .range([0, width - (margin.right+margin.left)])
)}

function _yScale(d3,margin,height){return(
    // d3.scaleSymlog()
    d3.scaleSymlog()
        .range([height - (margin.bottom+margin.top), margin.bottom])
)}

function _updateAxis(d3,xAxisCall,yAxisCall){return(
    function updateAxis(){
        let t = d3.transition()
            .duration(500)

        d3.select("#xaxis")
            .transition(t)
            .call(xAxisCall)

        d3.select("#yaxis")
            .transition(t)
            .call(yAxisCall)
    }
)}

// function _24(md){return(
//     md`### Constants`
// )}

function _height(){return(
    800
)}

function _margin(){return(
    {
        top: 10,
        bottom: 65,
        left: 130,
        right: 150
    }
)}

// function _27(md){return(
//     md`### Imports`
// )}

function _d3legend(require){return(
    require('d3-svg-legend@2.25.6/indexRollup.js')
)}

function _d3(require){return(
    require('d3@5')
)}

export default function define(runtime, observer) {
    const main = runtime.module();
    function toString() { return this.url; }

    const fileAttachments = new Map([
        ["example_data2.csv", {url: "files/churn.csv", mimeType: "text/csv", toString}]
        // ["example_data2.csv", {url: "example_data2.csv", mimeType: "text/csv", toString}]
    ]);

    main.builtin("FileAttachment", runtime.fileAttachments(name => fileAttachments.get(name)));
    // main.variable(observer()).define(["md"], _1);
    main.variable(observer("chart")).define("chart", ["d3","DOM","width","height"], _chart);
    // main.variable(observer("viewof start")).define("viewof start", ["slider"], _start);
    // main.variable(observer("start")).define("start", ["Generators", "viewof start"], (G, _) => G.input(_));
    // main.variable(observer("viewof datawidth")).define("viewof datawidth", ["slider"], _datawidth);
    // main.variable(observer("datawidth")).define("datawidth", ["Generators", "viewof datawidth"], (G, _) => G.input(_));
    // main.variable(observer()).define(["md"], _5);
    main.variable(observer("csvdata")).define("csvdata", ["d3","FileAttachment"], _csvdata);
    main.variable(observer("initialSlice")).define("initialSlice", ["csvdata"], _initialSlice);
    // main.variable(observer("subset")).define("subset", ["csvdata","start"], _subset);
    main.variable(observer()).define(["d3","chart","width","margin","height","d3legend","colorScale","rScale","setScale","initialSlice","initAxis","md","update"], _9);
    // main.variable(observer()).define(["setScale","subset","update","md"], _10);
    // main.variable(observer()).define(["md"], _11);
    main.variable(observer("opacityScale")).define("opacityScale", ["d3","csvdata"], _opacityScale);
    main.variable(observer("colorScale")).define("colorScale", ["d3","csvdata"], _colorScale);
    main.variable(observer("rScale")).define("rScale", ["d3","csvdata"], _rScale);
    main.variable(observer("initAxis")).define("initAxis", ["d3","chart","margin","height","xAxisCall","width","yAxisCall"], _initAxis);
    // main.variable(observer("formatTime")).define("formatTime", ["d3"], _formatTime);
    // main.variable(observer("parseTime")).define("parseTime", ["d3"], _parseTime);
    main.variable(observer("setScale")).define("setScale", ["d3","xScale","xAxisCall","yScale","yAxisCall"], _setScale);
    main.variable(observer("update")).define("update", ["d3","xScale","yScale","rScale","opacityScale", "colorScale","updateAxis"], _update);
    main.variable(observer("xAxisCall")).define("xAxisCall", ["d3"], _xAxisCall);
    main.variable(observer("yAxisCall")).define("yAxisCall", ["d3"], _yAxisCall);
    main.variable(observer("xScale")).define("xScale", ["d3","width","margin"], _xScale);
    main.variable(observer("yScale")).define("yScale", ["d3","margin","height"], _yScale);
    main.variable(observer("updateAxis")).define("updateAxis", ["d3","xAxisCall","yAxisCall"], _updateAxis);
    // main.variable(observer()).define(["md"], _24);
    main.variable(observer("height")).define("height", _height);
    main.variable(observer("margin")).define("margin", _margin);
    // main.variable(observer()).define(["md"], _27);

    const child1 = runtime.module(define1);
    main.import("slider", child1);

    main.variable(observer("d3legend")).define("d3legend", ["require"], _d3legend);
    main.variable(observer("d3")).define("d3", ["require"], _d3);

    return main;
}