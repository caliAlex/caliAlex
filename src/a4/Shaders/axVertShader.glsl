#version 430

uniform mat4 axes_mv_matrix;
uniform mat4 axes_proj_matrix;

out vec4 axisColor;

void main(){
  //Axis lines are connected through each set of vertices with
  // origin at 0x, 0y, 0z
  const vec4 vertices[6] = vec4[6]
  (vec4(0.0, 0.0, 0.0, 1.0),  //0
  vec4(50.0, 0.0, 0.0, 1.0),  //1

  vec4(0.0, 0.0, 0.0, 1.0),   //2
  vec4(0.0, 50.0, 0.0, 1.0),  //3

  vec4(0.0, 0.0, 0.0, 1.0),   //4
  vec4(0.0, 0.0, 50.0, 1.0)); //5


  //Ensures each axis will be updated with each call to didplay()
  // through uniform variable passing
  gl_Position = axes_proj_matrix * axes_mv_matrix * vertices[gl_VertexID];

  //Each axis is based on the vertex location stored at each index
  if (gl_VertexID == 0 || gl_VertexID == 1)
    axisColor = vec4(1.0, 0.0, 0.0, 1.0);
  else if (gl_VertexID == 2 || gl_VertexID == 3)
    axisColor = vec4(0.0, 1.0, 0.0, 1.0);
  else
    axisColor = vec4(0.0, 0.0, 1.0, 1.0);
}
