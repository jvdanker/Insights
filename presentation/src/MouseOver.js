import * as d3 from "d3";

function addMouseOver(plot_g, config, data, x, y) {
    const plot_width = config.svg_width - config.plot_margin.left - config.plot_margin.right;
    const plot_height = config.svg_height - config.plot_margin.top - config.plot_margin.bottom;

    const svg = d3
        .select("svg");

    const mouse_g = plot_g
        .append('g')
        .classed('mouse', true)
        .style('display', 'none');

    mouse_g
        .append('rect')
        .attr('width', 2)
        .attr('x',-1)
        .attr('height', plot_height)
        .attr('fill', 'lightgray');

    mouse_g
        .append('circle')
        .attr('r', 3)
        .attr("stroke", "steelblue");

    mouse_g
        .append('text');

    const [min, max] = d3.extent(data, d=>d.epoch);

    plot_g.on("mouseover", () => mouse_g.style('display', 'block'));
    plot_g.on("mouseout", () => mouse_g.style('display', 'none'));

    plot_g.on("mousemove", function(event) {
        const [x_cord,y_cord] = d3.pointer(event);
        const ratio = x_cord / plot_width;
        const currentDateX = new Date(+min + Math.round(ratio * (max - min)));
        const index = d3.bisectCenter(data.map(d => d.epoch), currentDateX);
        const current = data[index];
        const transX = x(current.epoch);

        mouse_g
            .attr('transform', `translate(${transX},${0})`);

        mouse_g
            .select('text')
            .text(`${current.epoch.toLocaleDateString()}, ${current.count}`)
            .attr('text-anchor', current.epoch < (min + max) / 2 ? "start" : "end");

        mouse_g
            .select('circle')
            .attr('cy', y(current.count));
    });
}

export default addMouseOver;